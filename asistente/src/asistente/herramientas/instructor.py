import dataclasses
import datetime

from asistente import backend
from asistente.herramientas import registro
from asistente.herramientas.registro import claims_del_token, herramienta, resultado


def _deshacer_crear_curso(args: dict, res: dict) -> dict | None:
    if not res.get("ok"):
        return None
    datos = res.get("datos")
    curso_id = datos.get("id") if isinstance(datos, dict) else None
    if curso_id is None:
        return None
    return {
        "metodo": "DELETE",
        "ruta": f"/courses/{curso_id}",
        "descripcion": "Borrar el curso recién creado",
    }


@herramienta(
    "mis_cursos",
    "Lista los cursos del instructor. Lo usa el INSTRUCTOR; no necesita datos.",
    {"INSTRUCTOR"}, "lectura",
)
async def mis_cursos(token: str | None) -> dict:
    claims = claims_del_token(token)
    user_id = claims.get("userId") if claims else None
    if user_id is None:
        return {"ok": False, "status": 401, "code": None,
                "mensaje": backend.MENSAJE_401}
    r = await backend.llamar("GET", "/api/courses", token, base_url=registro.BACKEND_URL)
    if r.ok and isinstance(r.datos, list):
        r = dataclasses.replace(
            r, datos=[c for c in r.datos if c.get("instructorId") == user_id])
    return resultado(r)


@herramienta(
    "ver_mi_curso",
    "Muestra el detalle de un curso propio. Lo usa el INSTRUCTOR. Dato: curso_id.",
    {"INSTRUCTOR"}, "lectura",
)
async def ver_mi_curso(token: str | None, curso_id: int) -> dict:
    return resultado(await backend.llamar(
        "GET", f"/instructor/courses/{curso_id}", token, base_url=registro.BACKEND_URL))


@herramienta(
    "alumnos_de_mi_curso",
    "Lista los alumnos inscriptos en un curso propio. Lo usa el INSTRUCTOR. "
    "Dato: curso_id.",
    {"INSTRUCTOR"}, "lectura",
)
async def alumnos_de_mi_curso(token: str | None, curso_id: int) -> dict:
    return resultado(await backend.llamar(
        "GET", f"/instructor/courses/{curso_id}/students", token,
        base_url=registro.BACKEND_URL))


@herramienta(
    "crear_curso",
    "Crea un curso nuevo en borrador. Lo usa el INSTRUCTOR. Datos: titulo, "
    "descripcion (opcional), precio_base, fecha_inicio, fecha_fin (YYYY-MM-DD), "
    "cupo y especialidad_id.",
    {"INSTRUCTOR"}, "escritura", deshacer=_deshacer_crear_curso,
)
async def crear_curso(
    token: str | None, titulo: str, descripcion: str | None, precio_base: float,
    fecha_inicio: datetime.date, fecha_fin: datetime.date, cupo: int,
    especialidad_id: int,
) -> dict:
    body: dict = {
        "title": titulo,
        "basePrice": precio_base,
        "startDate": fecha_inicio.isoformat(),
        "endDate": fecha_fin.isoformat(),
        "maxCapacity": cupo,
        "specialtyId": especialidad_id,
    }
    if descripcion is not None:
        body["description"] = descripcion
    return resultado(await backend.llamar(
        "POST", "/instructor/courses", token, base_url=registro.BACKEND_URL, json=body))


@herramienta(
    "editar_curso",
    "Modifica un curso propio; solo cambia los datos indicados. Lo usa el "
    "INSTRUCTOR. Datos: curso_id y, opcionales, titulo, descripcion, precio_base, "
    "fecha_inicio, fecha_fin, cupo, especialidad_id.",
    {"INSTRUCTOR"}, "escritura",
)
async def editar_curso(
    token: str | None, curso_id: int, titulo: str | None = None,
    descripcion: str | None = None, precio_base: float | None = None,
    fecha_inicio: datetime.date | None = None, fecha_fin: datetime.date | None = None,
    cupo: int | None = None, especialidad_id: int | None = None,
) -> dict:
    candidatos = {
        "title": titulo,
        "description": descripcion,
        "basePrice": precio_base,
        "startDate": fecha_inicio.isoformat() if fecha_inicio else None,
        "endDate": fecha_fin.isoformat() if fecha_fin else None,
        "maxCapacity": cupo,
        "specialtyId": especialidad_id,
    }
    body = {k: v for k, v in candidatos.items() if v is not None}
    return resultado(await backend.llamar(
        "PUT", f"/instructor/courses/{curso_id}", token,
        base_url=registro.BACKEND_URL, json=body))


@herramienta(
    "borrar_curso",
    "Borra un curso propio. Lo usa el INSTRUCTOR. Dato: curso_id.",
    {"INSTRUCTOR"}, "borrado",
)
async def borrar_curso(token: str | None, curso_id: int) -> dict:
    return resultado(await backend.llamar(
        "DELETE", f"/instructor/courses/{curso_id}", token,
        base_url=registro.BACKEND_URL))


@herramienta(
    "pedir_aprobacion",
    "Envía un curso propio a aprobación del administrador. Lo usa el INSTRUCTOR. "
    "Datos: curso_id y comentario (opcional).",
    {"INSTRUCTOR"}, "escritura",
)
async def pedir_aprobacion(
    token: str | None, curso_id: int, comentario: str | None = None,
) -> dict:
    body = {"instructorComment": comentario} if comentario is not None else {}
    return resultado(await backend.llamar(
        "POST", f"/instructor/courses/{curso_id}/approval-requests", token,
        base_url=registro.BACKEND_URL, json=body))


@herramienta(
    "mis_solicitudes",
    "Lista las solicitudes de aprobación del instructor. Lo usa el INSTRUCTOR; "
    "no necesita datos.",
    {"INSTRUCTOR"}, "lectura",
)
async def mis_solicitudes(token: str | None) -> dict:
    return resultado(await backend.llamar(
        "GET", "/instructor/approval-requests", token, base_url=registro.BACKEND_URL))


@herramienta(
    "mi_recaudacion",
    "Muestra la recaudación por inscripciones del instructor. Lo usa el "
    "INSTRUCTOR. Datos opcionales: desde y hasta (YYYY-MM-DD).",
    {"INSTRUCTOR"}, "lectura",
)
async def mi_recaudacion(
    token: str | None, desde: datetime.date | None = None,
    hasta: datetime.date | None = None,
) -> dict:
    params = {}
    if desde is not None:
        params["fromDate"] = desde.isoformat()
    if hasta is not None:
        params["toDate"] = hasta.isoformat()
    return resultado(await backend.llamar(
        "GET", "/instructor/revenue/enrollments", token,
        base_url=registro.BACKEND_URL, params=params or None))
