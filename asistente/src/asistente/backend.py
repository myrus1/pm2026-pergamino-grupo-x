from dataclasses import dataclass
from typing import Any

import httpx

TIMEOUT_SEGUNDOS = 15
MENSAJE_401 = "Tu sesión venció o no iniciaste sesión."


@dataclass
class ResultadoApi:
    ok: bool
    status: int
    datos: Any
    code: str | None
    mensaje: str | None


def _cabecera_auth(token: str | None) -> dict[str, str]:
    if not token:
        return {}
    jwt = token.strip()
    if jwt.lower().startswith("bearer "):
        jwt = jwt[7:].strip()
    return {"Authorization": f"Bearer {jwt}"} if jwt else {}


def _json_o_none(resp: httpx.Response) -> Any:
    if not resp.content:
        return None
    try:
        return resp.json()
    except ValueError:
        return None


def _mensaje_error(status: int, cuerpo: Any) -> tuple[str | None, str]:
    if not isinstance(cuerpo, dict):
        return None, f"Error del backend ({status})"
    mensaje = cuerpo.get("detail") or cuerpo.get("title") or f"Error del backend ({status})"
    errores = cuerpo.get("errors")
    if isinstance(errores, list):
        detalles = [
            f"{e.get('field')}: {e.get('message')}" for e in errores if isinstance(e, dict)
        ]
        if detalles:
            mensaje = f"{mensaje} " + "; ".join(detalles)
    return cuerpo.get("code"), mensaje


async def llamar(
    metodo: str,
    ruta: str,
    token: str | None,
    *,
    json: dict | None = None,
    params: dict | None = None,
    base_url: str,
) -> ResultadoApi:
    url = base_url.rstrip("/") + ruta
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT_SEGUNDOS) as cliente:
            resp = await cliente.request(
                metodo, url, headers=_cabecera_auth(token), json=json, params=params
            )
    except httpx.HTTPError:
        return ResultadoApi(False, 0, None, None, "No se pudo conectar con el backend")

    cuerpo = _json_o_none(resp)
    if resp.is_success:
        return ResultadoApi(True, resp.status_code, cuerpo, None, None)
    if resp.status_code == 401:
        code = cuerpo.get("code") if isinstance(cuerpo, dict) else None
        return ResultadoApi(False, 401, None, code, MENSAJE_401)
    code, mensaje = _mensaje_error(resp.status_code, cuerpo)
    return ResultadoApi(False, resp.status_code, None, code, mensaje)
