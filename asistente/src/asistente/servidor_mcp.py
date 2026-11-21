import inspect
import json
from functools import wraps

from mcp.server.mcpserver import Context, MCPServer
from mcp.types import ToolAnnotations

from asistente import backend
from asistente import herramientas  # noqa: F401  (puebla REGISTRO)
from asistente.herramientas import registro
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


URI_CATALOGO = "pm2026://cursos"
CAMPOS_CATALOGO = ("id", "title", "description", "basePrice", "startDate", "endDate", "maxCapacity")


async def catalogo_de_cursos() -> str:
    """Cursos publicados, sin token: es la misma lista pública que ve cualquiera."""
    r = await backend.llamar("GET", "/api/courses", None, base_url=registro.BACKEND_URL)
    if not r.ok or not isinstance(r.datos, list):
        return json.dumps({"ok": False, "mensaje": r.mensaje or "No se pudo leer el catálogo."},
                          ensure_ascii=False)
    cursos = [{k: c.get(k) for k in CAMPOS_CATALOGO} for c in r.datos
              if isinstance(c, dict) and c.get("status") == "PUBLISHED"]
    return json.dumps(cursos, ensure_ascii=False, default=str)


def _agregar_recursos(servidor: MCPServer) -> None:
    servidor.resource(
        URI_CATALOGO, name="catalogo_de_cursos", title="Catálogo de cursos publicados",
        description="Los cursos publicados de la plataforma (título, precio, fechas y cupo). "
                    "Es de solo lectura y no necesita sesión.",
        mime_type="application/json",
    )(catalogo_de_cursos)


# Los prompts son pedidos armados: el usuario elige uno y el cliente se lo pasa al modelo,
# que resuelve con las herramientas. Ninguno pide cambiar datos.
def resumen_recaudacion() -> str:
    return (
        "Armame un resumen de la recaudación de la plataforma. Usá reporte_recaudacion para el "
        "total y reporte_recaudacion_por_curso para el detalle. Decime el total, los tres cursos "
        "que más recaudaron y los que no recaudaron nada. Solo leé datos: no cambies nada."
    )


def recomendar_curso(tema: str) -> str:
    return (
        f"Quiero aprender sobre «{tema}». Buscá cursos con buscar_cursos (o mirá el catálogo "
        f"{URI_CATALOGO}) y recomendame hasta tres, con precio y fecha de inicio, explicando por "
        "qué cada uno. No me inscribas: solo recomendame."
    )


def revisar_solicitudes() -> str:
    return (
        "Listá las solicitudes de aprobación pendientes con solicitudes_pendientes. Para cada una "
        "decime el curso, el instructor y si le falta algo (descripción, precio, fechas o cupo). "
        "No apruebes ni rechaces nada: eso lo decido yo."
    )


def _agregar_prompts(servidor: MCPServer) -> None:
    servidor.prompt(
        title="Resumen de recaudación",
        description="Total recaudado y los cursos que más y menos recaudaron. Para ADMINISTRADOR.",
    )(resumen_recaudacion)
    servidor.prompt(
        title="Recomendar un curso",
        description="Busca cursos sobre un tema y recomienda hasta tres. Para ALUMNO.",
    )(recomendar_curso)
    servidor.prompt(
        title="Revisar solicitudes pendientes",
        description="Resume las solicitudes de aprobación sin aprobar ni rechazar. Para ADMINISTRADOR.",
    )(revisar_solicitudes)


def crear_mcp() -> MCPServer:
    servidor = MCPServer("pm2026")
    _agregar_recursos(servidor)
    _agregar_prompts(servidor)
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
