"""Cliente de herramientas: el agente llama a las herramientas a través del /mcp propio."""
import json
import logging
from typing import Protocol

import httpx2
from mcp import Client
from mcp.client.streamable_http import streamable_http_client
from mcp.types import CallToolResult, TextContent

log = logging.getLogger(__name__)


class ClienteHerramientas(Protocol):
    async def llamar(self, nombre: str, args: dict) -> dict: ...


def _error(mensaje: str) -> dict:
    return {"ok": False, "status": None, "code": None, "mensaje": mensaje}


def resultado_mcp(r: CallToolResult) -> dict:
    """Convierte el resultado MCP (JSON en un bloque de texto) en el dict de la herramienta."""
    texto = "".join(c.text for c in r.content if isinstance(c, TextContent))
    if r.is_error:
        return _error(texto or "La herramienta falló.")
    if isinstance(r.structured_content, dict) and "ok" in r.structured_content:
        return r.structured_content
    try:
        datos = json.loads(texto)
    except ValueError:
        return _error("Respuesta inválida de la herramienta.")
    return datos if isinstance(datos, dict) else _error("Respuesta inválida de la herramienta.")


class ClienteMcpHttp:
    """Llama al /mcp propio (`{base_url}/mcp`) con el Authorization del usuario."""

    def __init__(
        self, base_url: str, token: str | None, *,
        transport: httpx2.AsyncBaseTransport | None = None, timeout: float = 30.0,
    ):
        self.url = base_url.rstrip("/") + "/mcp"
        self._headers = {"Authorization": token} if token else {}
        self._transport = transport
        self._timeout = timeout

    async def llamar(self, nombre: str, args: dict) -> dict:
        try:
            async with httpx2.AsyncClient(
                headers=self._headers, transport=self._transport, timeout=self._timeout,
            ) as http:
                async with Client(streamable_http_client(self.url, http_client=http)) as c:
                    r = await c.call_tool(nombre, args)
        except Exception:
            # Sin args en el log: pueden traer datos personales.
            log.exception("Falló la llamada MCP a %s", nombre)
            return _error("No pude ejecutar la herramienta.")
        return resultado_mcp(r)
