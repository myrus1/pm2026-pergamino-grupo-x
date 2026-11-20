# API del SDK `mcp` instalado: version 2.3.0 (mcp 2.x; difiere de mcp 1.x).
#
# - FastMCP se renombro: `from mcp.server.mcpserver import MCPServer`.
#   `mcp.server.fastmcp` ya no existe (importarlo lanza ModuleNotFoundError).
#   `Context` se importa de `mcp.server.mcpserver` (`from mcp.server.mcpserver import Context`).
# - Modo sin sesion con respuesta JSON: se pasan al metodo que arma la app, no al
#   constructor: `MCPServer("nombre").streamable_http_app(stateless_http=True, json_response=True)`.
#   Otros params utiles: `streamable_http_path="/mcp"`, `host="127.0.0.1"`,
#   `transport_security=TransportSecuritySettings(...)`.
# - La app ASGI de Streamable HTTP es `server.streamable_http_app(...)`: devuelve una
#   `Starlette` con una unica ruta, por defecto `/mcp` (cambia con `streamable_http_path`).
# - Lifespan: la app devuelta necesita su session manager corriendo. Al montarla en FastAPI
#   hay que correrlo en el lifespan de FastAPI:
#       @asynccontextmanager
#       async def lifespan(app):
#           async with servidor.session_manager.run():
#               yield
#   `servidor.session_manager` solo existe despues de llamar a `streamable_http_app()`
#   (si no, RuntimeError) y `run()` solo se puede llamar una vez por instancia.
# - Headers del pedido HTTP dentro de una herramienta: declarar un parametro `ctx: Context`
#   y leer `ctx.headers` (Mapping[str, str] | None; None en stdio), p. ej.
#   `(ctx.headers or {}).get("authorization")`. Los headers vienen en minusculas.
#   Tambien esta `ctx.request_context.request` (el request de Starlette).
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import datetime
import json
import logging
import re
import unicodedata

from fastapi import Depends, FastAPI, Form, Header, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from mcp.server.transport_security import TransportSecuritySettings

from asistente.agente import (
    MAX_TURNOS_HISTORIAL, PENDIENTES, AsistenteFallo, AsistenteNoConfigurado, AsistenteSaturado,
    ClienteHerramientas, Modelo, ModeloOpenAI, confirmar, ejecutar,
)
from asistente.cliente_mcp import ClienteMcpHttp
from asistente.config import Settings, cargar
from asistente.servidor_mcp import crear_mcp
from asistente.transcripcion import transcribir

MAX_AUDIO = 10 * 1024 * 1024
MENSAJE_SATURADO = "El asistente está saturado, probá en un minuto."
MENSAJE_NO_CONFIGURADO = "El asistente no está configurado."
MENSAJE_NO_ENTENDI = "No te entendí, probá de nuevo."
MENSAJE_FALLO = "El asistente tuvo un problema, probá de nuevo."
MENSAJE_ERROR_INTERNO = "Error interno del asistente."

log = logging.getLogger(__name__)


class ErroresInternos:
    """Convierte una excepción no manejada en un JSON 500.

    Se registra ANTES que CORSMiddleware, así queda adentro y la respuesta de error sale con
    `Access-Control-Allow-Origin`. (El handler de `Exception` de Starlette vive en
    ServerErrorMiddleware, afuera de CORS, y su 500 no tendría ese header.) Se loguea solo el
    tipo de la excepción: nunca el cuerpo del pedido ni el mensaje, que podría traer datos.
    """

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        empezo = False

        async def enviar(mensaje):
            nonlocal empezo
            if mensaje["type"] == "http.response.start":
                empezo = True
            await send(mensaje)

        try:
            await self.app(scope, receive, enviar)
        except Exception as e:
            log.error("Error interno en %s: %s", scope.get("path"), type(e).__name__)
            if empezo:
                raise
            await JSONResponse({"detail": MENSAJE_ERROR_INTERNO}, status_code=500)(
                scope, receive, send)
AFIRMATIVOS = frozenset({"si", "dale", "confirmo", "borralo"})


def obtener_settings(request: Request) -> Settings:
    return request.app.state.settings


def obtener_transcriptor(request: Request):
    settings = obtener_settings(request)

    async def transcriptor(audio: bytes, nombre: str, tipo: str) -> str:
        return await transcribir(audio, nombre, tipo, settings=settings)
    return transcriptor


def obtener_modelo(request: Request) -> Modelo:
    return ModeloOpenAI(obtener_settings(request))


def obtener_herramientas(
    request: Request, authorization: str | None = Header(default=None),
) -> ClienteHerramientas:
    # El /mcp propio, con el Authorization de ESTE pedido.
    return ClienteMcpHttp(f"http://127.0.0.1:{obtener_settings(request).port}", authorization)


def _normalizar(texto: str) -> str:
    sin_tildes = "".join(
        c for c in unicodedata.normalize("NFD", texto.lower()) if unicodedata.category(c) != "Mn")
    return re.sub(r"[^\w\s]", " ", sin_tildes).strip()


def _es_afirmativo(texto: str) -> bool:
    palabras = _normalizar(texto).split()
    return bool(palabras) and palabras[0] in AFIRMATIVOS


def _historial(crudo: str | None) -> list[dict]:
    if not crudo:
        return []
    try:
        lista = json.loads(crudo)
    except ValueError:
        lista = None
    if not isinstance(lista, list):
        raise HTTPException(400, "historial inválido")
    return [t for t in lista if isinstance(t, dict)][-MAX_TURNOS_HISTORIAL:]


async def _voz(audio, historial, pendiente_id, confirmar_, token, transcriptor, modelo,
               herramientas) -> dict:
    turnos = _historial(historial)
    if audio is None:
        if confirmar_ == "true" and pendiente_id:
            return {"transcripcion": None, **await confirmar(pendiente_id, token, herramientas)}
        raise HTTPException(400, "Falta el audio.")
    datos = await audio.read(MAX_AUDIO + 1)
    if len(datos) > MAX_AUDIO:
        raise HTTPException(400, "El audio es demasiado largo.")
    texto = await transcriptor(
        datos, audio.filename or "audio", audio.content_type or "application/octet-stream")
    if not texto.strip():
        return {"transcripcion": texto.strip(), "respuesta": MENSAJE_NO_ENTENDI, "acciones": [],
                "datos": None, "deshacer": None, "pendiente": None, "secreto": None}
    if pendiente_id:
        if _es_afirmativo(texto):
            return {"transcripcion": texto, **await confirmar(pendiente_id, token, herramientas)}
        PENDIENTES.pop(pendiente_id, None)
    resultado = await ejecutar(
        texto, token, turnos, modelo=modelo, herramientas=herramientas,
        hoy=datetime.date.today())
    return {"transcripcion": texto, **resultado}


def crear_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or cargar()
    # Una instancia de MCP por app: session_manager.run() solo se puede llamar una vez.
    servidor = crear_mcp()
    # Sin proteccion DNS-rebinding: por defecto solo acepta Host localhost y rompe en un host publico.
    mcp_app = servidor.streamable_http_app(
        stateless_http=True,
        json_response=True,
        transport_security=TransportSecuritySettings(enable_dns_rebinding_protection=False),
    )

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        async with servidor.session_manager.run():
            yield

    app = FastAPI(title="Asistente de voz", lifespan=lifespan)
    app.state.settings = settings
    # Orden: el último agregado queda afuera. CORS envuelve a ErroresInternos.
    app.add_middleware(ErroresInternos)
    app.add_middleware(
        CORSMiddleware, allow_origins=list(settings.allowed_origins),
        allow_methods=["POST"], allow_headers=["Authorization", "Content-Type"],
    )

    @app.get("/salud")
    def salud() -> dict[str, str]:
        return {"estado": "ok"}

    @app.post("/voz")
    async def voz(
        audio: UploadFile | None = None,
        historial: str | None = Form(default=None),
        pendiente_id: str | None = Form(default=None),
        confirmar_: str | None = Form(default=None, alias="confirmar"),
        authorization: str | None = Header(default=None),
        transcriptor=Depends(obtener_transcriptor),
        modelo: Modelo = Depends(obtener_modelo),
        herramientas: ClienteHerramientas = Depends(obtener_herramientas),
    ) -> JSONResponse:
        try:
            return JSONResponse(await _voz(
                audio, historial, pendiente_id, confirmar_, authorization,
                transcriptor, modelo, herramientas))
        except AsistenteNoConfigurado:
            return JSONResponse({"detail": MENSAJE_NO_CONFIGURADO}, status_code=503)
        except AsistenteSaturado:
            return JSONResponse({"detail": MENSAJE_SATURADO}, status_code=503)
        except AsistenteFallo:
            return JSONResponse({"detail": MENSAJE_FALLO}, status_code=502)

    # La sub-app ya expone /mcp; se monta al final para que /salud y demas rutas ganen.
    app.mount("/", mcp_app)
    return app


app = crear_app()
