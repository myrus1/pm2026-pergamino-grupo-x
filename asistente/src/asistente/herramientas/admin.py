import datetime

from asistente import backend
from asistente.herramientas import registro
from asistente.herramientas.registro import generar_contrasena, herramienta, resultado

ADMIN = {"ADMINISTRADOR"}


async def _get(token: str | None, ruta: str, params: dict | None = None):
    return await backend.llamar(
        "GET", ruta, token, base_url=registro.BACKEND_URL, params=params)


async def _delete(token: str | None, ruta: str) -> dict:
    return resultado(await backend.llamar(
        "DELETE", ruta, token, base_url=registro.BACKEND_URL))


async def _detalle_con_lista(token: str | None, ruta: str, clave: str) -> dict:
    """GET del detalle y de la sublista; devuelve el detalle con la lista adentro."""
    detalle = await _get(token, ruta)
    if not detalle.ok:
        return resultado(detalle)
    lista = resultado(await _get(token, f"{ruta}/{clave[1]}"))
    if not lista["ok"]:
        return lista
    datos = {**(detalle.datos or {}), clave[0]: lista["datos"],
             f"total_{clave[0]}": lista["total"]}
    return {"ok": True, "datos": datos}


def _id_creado(res: dict) -> int | None:
    if not res.get("ok"):
        return None
    datos = res.get("datos")
    return datos.get("id") if isinstance(datos, dict) else None


def _deshacer_borrando(prefijo: str, descripcion: str):
    def deshacer(args: dict, res: dict) -> dict | None:
        id_ = _id_creado(res)
        if id_ is None:
            return None
        return {"metodo": "DELETE", "ruta": f"{prefijo}/{id_}", "descripcion": descripcion}
    return deshacer


def _deshacer_asignar(args: dict, res: dict) -> dict | None:
    if not res.get("ok"):
        return None
    ins, esp = args.get("instructor_id"), args.get("especialidad_id")
    if ins is None or esp is None:
        return None
    return {"metodo": "DELETE", "ruta": f"/instructors/{ins}/specialties/{esp}",
            "descripcion": "Quitar la especialidad recién asignada"}


# --- Alumnos ---

@herramienta(
    "listar_alumnos",
    "Lista los alumnos. Lo usa el ADMINISTRADOR; no necesita datos.",
    ADMIN, "lectura",
)
async def listar_alumnos(token: str | None) -> dict:
    return resultado(await _get(token, "/admin/students"))


@herramienta(
    "ver_alumno",
    "Muestra el detalle de un alumno y los cursos en que está inscripto. Lo usa el "
    "ADMINISTRADOR. Dato: id del alumno.",
    ADMIN, "lectura",
)
async def ver_alumno(token: str | None, id: int) -> dict:
    return await _detalle_con_lista(token, f"/admin/students/{id}", ("cursos", "courses"))


@herramienta(
    "borrar_alumno",
    "Borra un alumno. Lo usa el ADMINISTRADOR. Dato: id del alumno.",
    ADMIN, "borrado",
)
async def borrar_alumno(token: str | None, id: int) -> dict:
    return await _delete(token, f"/api/students/{id}")


# --- Instructores ---

@herramienta(
    "listar_instructores",
    "Lista los instructores. Lo usa el ADMINISTRADOR; no necesita datos.",
    ADMIN, "lectura",
)
async def listar_instructores(token: str | None) -> dict:
    return resultado(await _get(token, "/admin/instructors"))


@herramienta(
    "ver_instructor",
    "Muestra el detalle de un instructor. Lo usa el ADMINISTRADOR. Dato: id del "
    "instructor.",
    ADMIN, "lectura",
)
async def ver_instructor(token: str | None, id: int) -> dict:
    return resultado(await _get(token, f"/admin/instructors/{id}"))


@herramienta(
    "crear_instructor",
    "Crea un instructor. Lo usa el ADMINISTRADOR. Datos: nombre, apellido, email. "
    "La contraseña se genera sola.",
    ADMIN, "escritura",
    deshacer=_deshacer_borrando("/instructors", "Borrar el instructor recién creado"),
)
async def crear_instructor(
    token: str | None, nombre: str, apellido: str, email: str
) -> dict:
    contrasena = generar_contrasena()
    r = await backend.llamar(
        "POST", "/api/instructors", token, base_url=registro.BACKEND_URL,
        json={"firstName": nombre, "lastName": apellido, "email": email,
              "password": contrasena},
    )
    res = resultado(r)
    if res["ok"]:
        res["contrasena"] = contrasena
    return res


@herramienta(
    "borrar_instructor",
    "Borra un instructor. Lo usa el ADMINISTRADOR. Dato: id del instructor.",
    ADMIN, "borrado",
)
async def borrar_instructor(token: str | None, id: int) -> dict:
    return await _delete(token, f"/api/instructors/{id}")


# --- Administradores ---

@herramienta(
    "listar_administradores",
    "Lista los administradores. Lo usa el ADMINISTRADOR; no necesita datos.",
    ADMIN, "lectura",
)
async def listar_administradores(token: str | None) -> dict:
    return resultado(await _get(token, "/admin/accounts"))


@herramienta(
    "crear_administrador",
    "Crea un administrador. Lo usa el ADMINISTRADOR. Datos: nombre, apellido, email. "
    "La contraseña se genera sola.",
    ADMIN, "escritura",
    deshacer=_deshacer_borrando("/administrators", "Borrar el administrador recién creado"),
)
async def crear_administrador(
    token: str | None, nombre: str, apellido: str, email: str
) -> dict:
    contrasena = generar_contrasena()
    r = await backend.llamar(
        "POST", "/admin/accounts", token, base_url=registro.BACKEND_URL,
        json={"email": email, "password": contrasena, "name": nombre,
              "lastName": apellido},
    )
    res = resultado(r)
    if res["ok"]:
        res["contrasena"] = contrasena
    return res


@herramienta(
    "borrar_administrador",
    "Borra un administrador. Lo usa el ADMINISTRADOR. Dato: id del administrador.",
    ADMIN, "borrado",
)
async def borrar_administrador(token: str | None, id: int) -> dict:
    return await _delete(token, f"/admin/accounts/{id}")


# --- Especialidades ---

@herramienta(
    "asignar_especialidad",
    "Asigna una especialidad a un instructor. Lo usa el ADMINISTRADOR. Datos: "
    "instructor_id y especialidad_id.",
    ADMIN, "escritura", deshacer=_deshacer_asignar,
)
async def asignar_especialidad(
    token: str | None, instructor_id: int, especialidad_id: int
) -> dict:
    return resultado(await backend.llamar(
        "PUT", f"/api/instructors/{instructor_id}/specialties/{especialidad_id}",
        token, base_url=registro.BACKEND_URL))


@herramienta(
    "quitar_especialidad",
    "Quita una especialidad a un instructor. Lo usa el ADMINISTRADOR. Datos: "
    "instructor_id y especialidad_id.",
    ADMIN, "borrado",
)
async def quitar_especialidad(
    token: str | None, instructor_id: int, especialidad_id: int
) -> dict:
    return await _delete(
        token, f"/api/instructors/{instructor_id}/specialties/{especialidad_id}")


@herramienta(
    "crear_especialidad",
    "Crea una especialidad nueva. Lo usa el ADMINISTRADOR. Dato: nombre.",
    ADMIN, "escritura",
    deshacer=_deshacer_borrando("/specialties", "Borrar la especialidad recién creada"),
)
async def crear_especialidad(token: str | None, nombre: str) -> dict:
    return resultado(await backend.llamar(
        "POST", "/api/specialties", token, base_url=registro.BACKEND_URL,
        json={"name": nombre}))


@herramienta(
    "borrar_especialidad",
    "Borra una especialidad. Lo usa el ADMINISTRADOR. Dato: id de la especialidad.",
    ADMIN, "borrado",
)
async def borrar_especialidad(token: str | None, id: int) -> dict:
    return await _delete(token, f"/api/specialties/{id}")


# --- Solicitudes de aprobación ---

@herramienta(
    "solicitudes_pendientes",
    "Lista las solicitudes de aprobación de cursos. Lo usa el ADMINISTRADOR; no "
    "necesita datos.",
    ADMIN, "lectura",
)
async def solicitudes_pendientes(token: str | None) -> dict:
    return resultado(await _get(token, "/admin/approval-requests"))


async def _resolver_solicitud(
    token: str | None, id: int, status: str, comentario: str | None
) -> dict:
    return resultado(await backend.llamar(
        "PUT", f"/admin/approval-requests/{id}", token, base_url=registro.BACKEND_URL,
        json={"status": status, "adminComment": comentario}))


@herramienta(
    "aprobar_solicitud",
    "Aprueba una solicitud de aprobación de curso. Lo usa el ADMINISTRADOR. Datos: id "
    "de la solicitud y comentario (opcional).",
    ADMIN, "escritura",
)
async def aprobar_solicitud(
    token: str | None, id: int, comentario: str | None = None
) -> dict:
    return await _resolver_solicitud(token, id, "APPROVED", comentario)


@herramienta(
    "rechazar_solicitud",
    "Rechaza una solicitud de aprobación de curso. Lo usa el ADMINISTRADOR. Datos: id "
    "de la solicitud y comentario (opcional).",
    ADMIN, "escritura",
)
async def rechazar_solicitud(
    token: str | None, id: int, comentario: str | None = None
) -> dict:
    return await _resolver_solicitud(token, id, "REJECTED", comentario)


# --- Cursos ---

@herramienta(
    "listar_cursos_admin",
    "Lista todos los cursos. Lo usa el ADMINISTRADOR; no necesita datos.",
    ADMIN, "lectura",
)
async def listar_cursos_admin(token: str | None) -> dict:
    return resultado(await _get(token, "/admin/courses"))


@herramienta(
    "ver_curso_admin",
    "Muestra el detalle de un curso y sus alumnos inscriptos. Lo usa el "
    "ADMINISTRADOR. Dato: id del curso.",
    ADMIN, "lectura",
)
async def ver_curso_admin(token: str | None, id: int) -> dict:
    return await _detalle_con_lista(token, f"/admin/courses/{id}", ("alumnos", "students"))


# --- Reportes ---

async def _reporte(
    token: str | None, ruta: str, desde: datetime.date, hasta: datetime.date
) -> dict:
    return resultado(await _get(
        token, ruta, {"fromDate": desde.isoformat(), "toDate": hasta.isoformat()}))


@herramienta(
    "reporte_recaudacion",
    "Reporte de recaudación total en un período. Lo usa el ADMINISTRADOR. Datos: "
    "desde y hasta (YYYY-MM-DD), obligatorios.",
    ADMIN, "lectura",
)
async def reporte_recaudacion(
    token: str | None, desde: datetime.date, hasta: datetime.date
) -> dict:
    return await _reporte(token, "/admin/reports/revenue", desde, hasta)


@herramienta(
    "reporte_recaudacion_por_curso",
    "Reporte de recaudación por curso en un período. Lo usa el ADMINISTRADOR. Datos: "
    "desde y hasta (YYYY-MM-DD), obligatorios.",
    ADMIN, "lectura",
)
async def reporte_recaudacion_por_curso(
    token: str | None, desde: datetime.date, hasta: datetime.date
) -> dict:
    return await _reporte(token, "/admin/reports/courses-revenue", desde, hasta)


@herramienta(
    "reporte_inscripciones",
    "Reporte de inscripciones en un período. Lo usa el ADMINISTRADOR. Datos: desde y "
    "hasta (YYYY-MM-DD), obligatorios.",
    ADMIN, "lectura",
)
async def reporte_inscripciones(
    token: str | None, desde: datetime.date, hasta: datetime.date
) -> dict:
    return await _reporte(token, "/admin/reports/enrollments", desde, hasta)
