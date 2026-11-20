"""Agente: texto -> modelo con las herramientas del rol -> MCP -> contrato de POST /voz.

Dos garantías de seguridad viven acá:
- solo se ejecutan herramientas de `para_rol(rol_del_token(token))`, aunque el modelo pida otra;
- las herramientas de tipo `borrado` nunca se ejecutan en `ejecutar`: quedan en `PENDIENTES`
  y solo `confirmar` (con el mismo token y antes de que venzan) las corre.
"""
import datetime
import functools
import hashlib
import hmac
import json
import secrets
from typing import Protocol, get_args

import openai
from mcp.server.mcpserver.tools.base import Tool

from asistente import herramientas as _herramientas  # noqa: F401  (puebla REGISTRO)
from asistente.cliente_mcp import ClienteHerramientas  # noqa: F401  (se re-exporta)
from asistente.config import Settings
from asistente.herramientas.registro import (
    REGISTRO, Herramienta, Rol, claims_del_token, para_rol,
)
from asistente.servidor_mcp import _envolver

MAX_LLAMADAS = 5
VIGENCIA_PENDIENTE = datetime.timedelta(minutes=5)
MENSAJE_NO_DISPONIBLE = "herramienta no disponible para tu rol"
MENSAJE_CORTE = "Hice lo que pude; el pedido era muy largo."
MENSAJE_NO_VALE = "Esa confirmación ya no vale."
MAX_TURNOS_HISTORIAL = 6
MENSAJE_UNA_CONTRASENA = "Una alta con contraseña por pedido; pedí la siguiente aparte."

# Herramientas cuyo resultado puede traer `contrasena`. Solo una por pedido: el contrato
# tiene un único `secreto` y una segunda contraseña se perdería.
GENERAN_CONTRASENA = frozenset({
    "registrar_alumno", "registrar_instructor", "crear_instructor", "crear_administrador",
})

ROLES_DEL_TOKEN = frozenset(r for r in get_args(Rol) if r != "PUBLICO")

# Quién puede ejecutar el borrado inverso de cada herramienta con `deshacer`.
# Si una herramienta no está, solo el ADMINISTRADOR.
ROLES_DESHACER: dict[str, frozenset[str]] = {
    "registrar_alumno": frozenset({"ADMINISTRADOR"}),
    "crear_instructor": frozenset({"ADMINISTRADOR"}),
    "crear_administrador": frozenset({"ADMINISTRADOR"}),
    "crear_especialidad": frozenset({"ADMINISTRADOR"}),
    "asignar_especialidad": frozenset({"ADMINISTRADOR"}),
    "crear_curso": frozenset({"INSTRUCTOR"}),
}

# id -> (herramienta, args, sha256(token), vence)
PENDIENTES: dict[str, tuple[str, dict, str, datetime.datetime]] = {}


class AsistenteSaturado(Exception):
    """El modelo o la transcripción devolvieron 429."""


class Modelo(Protocol):
    async def responder(self, mensajes: list[dict], herramientas: list[dict]) -> dict: ...


class ModeloOpenAI:
    def __init__(self, settings: Settings, *, http_client=None):
        self._modelo = settings.llm_model
        self._cliente = openai.AsyncOpenAI(
            base_url=settings.llm_base_url, api_key=settings.llm_api_key,
            http_client=http_client,
        )

    async def responder(self, mensajes: list[dict], herramientas: list[dict]) -> dict:
        extra = {"tools": herramientas, "tool_choice": "auto"} if herramientas else {}
        try:
            r = await self._cliente.chat.completions.create(
                model=self._modelo, messages=mensajes, **extra)
        except openai.RateLimitError as e:
            raise AsistenteSaturado() from e
        return r.choices[0].message.model_dump(exclude_none=True)


def rol_del_token(token: str | None) -> Rol | None:
    claims = claims_del_token(token)
    if not claims:
        return None
    rol = claims.get("rol")
    return rol if rol in ROLES_DEL_TOKEN else None


def _hash_token(token: str | None) -> str:
    return hashlib.sha256((token or "").encode()).hexdigest()


def _ahora() -> datetime.datetime:
    return datetime.datetime.now(datetime.UTC)


@functools.cache
def _parametros(nombre: str) -> dict:
    """JSON schema de los parámetros, el mismo que publica el servidor MCP (sin `token`)."""
    h = REGISTRO[nombre]
    return Tool.from_function(_envolver(h), name=h.nombre, description=h.descripcion).parameters


def esquema_openai(h: Herramienta) -> dict:
    return {"type": "function", "function": {
        "name": h.nombre, "description": h.descripcion, "parameters": _parametros(h.nombre)}}


def _system_prompt(rol: Rol | None, hoy: datetime.date) -> str:
    quien = rol or "visitante sin sesión"
    return (
        "Sos el asistente de voz de la plataforma de cursos. Respondé en castellano, breve "
        "(una o dos oraciones).\n"
        f"Hoy es {hoy.isoformat()}. El usuario es: {quien}.\n"
        "No inventes datos: si te falta un dato para usar una herramienta, pedíselo al usuario.\n"
        "No muestres contraseñas: el sistema se las muestra al usuario por su cuenta.\n"
        "Usá solo las herramientas que tenés disponibles."
    )


def _mensajes_historial(historial: list[dict]) -> list[dict]:
    roles = {"usuario": "user", "asistente": "assistant"}
    mensajes = []
    for turno in historial[-MAX_TURNOS_HISTORIAL:]:
        rol = roles.get(turno.get("rol"))
        texto = turno.get("texto")
        if rol and isinstance(texto, str):
            mensajes.append({"role": rol, "content": texto})
    return mensajes


def _legible(nombre: str) -> str:
    return nombre.replace("_", " ")


def _args_legibles(args: dict) -> str:
    return ", ".join(f"{k}={v}" for k, v in args.items())


def _con_args(texto: str, args: dict) -> str:
    return f"{texto} con {_args_legibles(args)}" if args else texto


def _pasado(nombre: str) -> str:
    """borrar_alumno -> 'borré alumno'."""
    verbo, _, resto = nombre.partition("_")
    if verbo.endswith("ar"):
        verbo = verbo[:-2] + "é"
    return f"{verbo} {_legible(resto)}".strip()


def _contrato(respuesta: str, acciones: list[dict] | None = None, datos=None, deshacer=None,
              pendiente=None, secreto=None) -> dict:
    return {"respuesta": respuesta, "acciones": acciones or [], "datos": datos,
            "deshacer": deshacer, "pendiente": pendiente, "secreto": secreto}


def _accion(h: Herramienta, ok: bool) -> dict:
    return {"herramienta": h.nombre, "ok": ok, "tipo": h.tipo}


def _args_de(call: dict) -> dict | None:
    crudos = (call.get("function") or {}).get("arguments")
    if isinstance(crudos, dict):
        return crudos
    if crudos in (None, ""):
        return {}
    try:
        args = json.loads(crudos)
    except (TypeError, ValueError):
        return None
    return args if isinstance(args, dict) else None


def _mensaje_tool(call_id: str, contenido: dict) -> dict:
    return {"role": "tool", "tool_call_id": call_id,
            "content": json.dumps(contenido, ensure_ascii=False, default=str)}


def _etiqueta_secreto(args: dict) -> str:
    nombre = " ".join(str(args[k]) for k in ("nombre", "apellido") if args.get(k))
    return f"Contraseña de {nombre}" if nombre else "Contraseña generada"


async def ejecutar(
    texto: str, token: str | None, historial: list[dict], *,
    modelo: Modelo, herramientas: ClienteHerramientas, hoy: datetime.date,
) -> dict:
    rol = rol_del_token(token)
    disponibles = {h.nombre: h for h in para_rol(rol)}
    esquemas = [esquema_openai(h) for h in disponibles.values()]
    mensajes = [{"role": "system", "content": _system_prompt(rol, hoy)},
                *_mensajes_historial(historial),
                {"role": "user", "content": texto}]

    acciones: list[dict] = []
    datos = deshacer = secreto = None
    llamadas = 0

    def contrato(respuesta: str, pendiente=None) -> dict:
        if secreto and secreto["valor"] in respuesta:
            respuesta = respuesta.replace(secreto["valor"], "********")
        return _contrato(respuesta, acciones, datos, deshacer, pendiente, secreto)

    for _ in range(MAX_LLAMADAS):
        msg = await modelo.responder(mensajes, esquemas)
        tool_calls = msg.get("tool_calls") or []
        if not tool_calls:
            return contrato(msg.get("content") or "")
        mensajes.append({"role": "assistant", "content": msg.get("content"),
                         "tool_calls": [
                             {"id": c.get("id"), "type": "function",
                              "function": {"name": (c.get("function") or {}).get("name"),
                                           "arguments": (c.get("function") or {})
                                           .get("arguments") or "{}"}}
                             for c in tool_calls]})
        for call in tool_calls:
            call_id = call.get("id")
            nombre = (call.get("function") or {}).get("name")
            h = disponibles.get(nombre)
            if h is None:
                mensajes.append(_mensaje_tool(call_id, {"ok": False,
                                                        "mensaje": MENSAJE_NO_DISPONIBLE}))
                continue
            args = _args_de(call)
            if args is None:
                mensajes.append(_mensaje_tool(call_id, {
                    "ok": False, "mensaje": "argumentos inválidos: mandá un objeto JSON"}))
                continue
            if h.tipo == "borrado":
                return contrato(*_guardar_pendiente(h, args, token))
            if secreto and h.nombre in GENERAN_CONTRASENA:
                mensajes.append(_mensaje_tool(call_id, {
                    "ok": False, "mensaje": MENSAJE_UNA_CONTRASENA}))
                continue
            if llamadas >= MAX_LLAMADAS:
                return contrato(MENSAJE_CORTE)
            llamadas += 1
            res = await herramientas.llamar(h.nombre, args)
            ok = bool(res.get("ok"))
            acciones.append(_accion(h, ok))
            contrasena = res.pop("contrasena", None)
            if contrasena:
                secreto = {"etiqueta": _etiqueta_secreto(args), "valor": str(contrasena)}
                res["contrasena"] = "generada; el sistema se la muestra al usuario"
            if ok and isinstance(res.get("datos"), list):
                datos = {"filas": res["datos"], "total": res.get("total", len(res["datos"]))}
            if ok and h.deshacer and rol in ROLES_DESHACER.get(h.nombre, {"ADMINISTRADOR"}):
                deshacer = h.deshacer(args, res) or deshacer
            mensajes.append(_mensaje_tool(call_id, res))
    return contrato(MENSAJE_CORTE)


def _guardar_pendiente(h: Herramienta, args: dict, token: str | None) -> tuple[str, dict]:
    ahora = _ahora()
    for pid in [p for p, e in PENDIENTES.items() if e[3] <= ahora]:
        PENDIENTES.pop(pid, None)
    pid = secrets.token_urlsafe(16)
    PENDIENTES[pid] = (h.nombre, dict(args), _hash_token(token), ahora + VIGENCIA_PENDIENTE)
    descripcion = f"¿Confirmás {_con_args(_legible(h.nombre), args)}?"
    return descripcion, {"id": pid, "descripcion": descripcion}


async def confirmar(
    pendiente_id: str, token: str | None, herramientas: ClienteHerramientas,
) -> dict:
    entrada = PENDIENTES.pop(pendiente_id, None)
    if entrada is None:
        return _contrato(MENSAJE_NO_VALE)
    nombre, args, hash_token, vence = entrada
    if not hmac.compare_digest(hash_token, _hash_token(token)) or vence <= _ahora():
        return _contrato(MENSAJE_NO_VALE)
    # Defensa extra: la herramienta tiene que seguir siendo un borrado del rol del token.
    h = {x.nombre: x for x in para_rol(rol_del_token(token))}.get(nombre)
    if h is None or h.tipo != "borrado":
        return _contrato(MENSAJE_NO_VALE)
    res = await herramientas.llamar(nombre, args)
    ok = bool(res.get("ok"))
    if ok:
        respuesta = f"Listo: {_con_args(_pasado(nombre), args)}."
    else:
        respuesta = res.get("mensaje") or "No se pudo completar."
    return _contrato(respuesta, [_accion(h, ok)])
