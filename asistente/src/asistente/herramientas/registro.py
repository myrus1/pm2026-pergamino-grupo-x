import base64
import json
import secrets
import string
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import Literal

from asistente import config
from asistente.backend import ResultadoApi

Rol = Literal["PUBLICO", "ALUMNO", "INSTRUCTOR", "ADMINISTRADOR"]
Tipo = Literal["lectura", "escritura", "borrado"]

BACKEND_URL = config.cargar().backend_url


@dataclass
class Herramienta:
    nombre: str
    descripcion: str
    roles: frozenset[Rol]
    tipo: Tipo
    funcion: Callable[..., Awaitable[dict]]
    deshacer: Callable[[dict, dict], dict | None] | None = None


REGISTRO: dict[str, Herramienta] = {}


def herramienta(
    nombre: str,
    descripcion: str,
    roles: set[Rol] | frozenset[Rol],
    tipo: Tipo,
    deshacer: Callable[[dict, dict], dict | None] | None = None,
):
    def decorador(funcion):
        REGISTRO[nombre] = Herramienta(
            nombre, descripcion, frozenset(roles), tipo, funcion, deshacer
        )
        return funcion

    return decorador


def para_rol(rol: Rol | None) -> list[Herramienta]:
    return [
        h for h in REGISTRO.values()
        if "PUBLICO" in h.roles or (rol is not None and rol in h.roles)
    ]


def resultado(r: ResultadoApi, limite: int = 20) -> dict:
    if not r.ok:
        return {"ok": False, "status": r.status, "code": r.code, "mensaje": r.mensaje}
    datos = r.datos
    if isinstance(datos, list):
        return {"ok": True, "datos": datos[:limite], "total": len(datos)}
    return {"ok": True, "datos": datos, "total": 1 if datos is not None else 0}


def claims_del_token(token: str | None) -> dict | None:
    """Decodifica el payload del JWT sin verificar la firma."""
    if not token:
        return None
    jwt = token.strip()
    if jwt.lower().startswith("bearer "):
        jwt = jwt[7:].strip()
    partes = jwt.split(".")
    if len(partes) != 3:
        return None
    try:
        payload = partes[1] + "=" * (-len(partes[1]) % 4)
        claims = json.loads(base64.urlsafe_b64decode(payload))
    except (ValueError, UnicodeDecodeError):
        return None
    return claims if isinstance(claims, dict) else None


def generar_contrasena() -> str:
    alfabeto = string.ascii_letters + string.digits
    return "".join(secrets.choice(alfabeto) for _ in range(10))
