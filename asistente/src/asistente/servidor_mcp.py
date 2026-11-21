import inspect
from functools import wraps

from mcp.server.mcpserver import Context, MCPServer
from mcp.types import ToolAnnotations

from asistente import herramientas  # noqa: F401  (puebla REGISTRO)
from asistente.herramientas.registro import REGISTRO, Herramienta


def token_del_pedido(ctx: Context) -> str | None:
    """Authorization del pedido HTTP actual (None si no hay pedido HTTP)."""
    return (ctx.headers or {}).get("authorization")


def _envolver(h: Herramienta):
    firma = inspect.signature(h.funcion)
    parametros = [p for n, p in firma.parameters.items() if n != "token"]
    parametros.append(
        inspect.Parameter("ctx", inspect.Parameter.KEYWORD_ONLY, annotation=Context)
    )

    @wraps(h.funcion)
    async def wrapper(*args, ctx: Context, **kwargs) -> dict:
        return await h.funcion(token_del_pedido(ctx), *args, **kwargs)

    wrapper.__signature__ = firma.replace(parameters=parametros, return_annotation=dict)
    wrapper.__annotations__ = {
        **{p.name: p.annotation for p in parametros},
        "return": dict,
    }
    return wrapper


def crear_mcp() -> MCPServer:
    servidor = MCPServer("pm2026")
    for h in REGISTRO.values():
        servidor.add_tool(
            _envolver(h),
            name=h.nombre,
            description=h.descripcion,
            annotations=ToolAnnotations(
                readOnlyHint=h.tipo == "lectura",
                destructiveHint=h.tipo == "borrado",
            ),
        )
    return servidor
