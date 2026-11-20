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

from fastapi import FastAPI
from mcp.server.transport_security import TransportSecuritySettings

from asistente.config import Settings, cargar
from asistente.servidor_mcp import crear_mcp


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

    @app.get("/salud")
    def salud() -> dict[str, str]:
        return {"estado": "ok"}

    # La sub-app ya expone /mcp; se monta al final para que /salud y demas rutas ganen.
    app.mount("/", mcp_app)
    return app


app = crear_app()
