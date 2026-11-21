from asistente import backend
from asistente.herramientas import registro
from asistente.herramientas.registro import herramienta, resultado


@herramienta(
    "cursos_disponibles",
    "Lista los cursos a los que el alumno puede inscribirse, con su precio. "
    "Lo usa el ALUMNO; no necesita datos.",
    {"ALUMNO"}, "lectura",
)
async def cursos_disponibles(token: str | None) -> dict:
    return resultado(await backend.llamar(
        "GET", "/student/courses/available", token, base_url=registro.BACKEND_URL))


@herramienta(
    "ver_curso",
    "Muestra el detalle de un curso. Lo usa el ALUMNO. Dato: curso_id.",
    {"ALUMNO"}, "lectura",
)
async def ver_curso(token: str | None, curso_id: int) -> dict:
    return resultado(await backend.llamar(
        "GET", f"/student/courses/{curso_id}", token, base_url=registro.BACKEND_URL))


@herramienta(
    "inscribirme",
    "Inscribe al alumno en un curso. Lo usa el ALUMNO. Dato: curso_id.",
    {"ALUMNO"}, "escritura",
)
async def inscribirme(token: str | None, curso_id: int) -> dict:
    return resultado(await backend.llamar(
        "POST", "/student/enrollments", token, base_url=registro.BACKEND_URL,
        json={"courseId": curso_id}))


@herramienta(
    "mis_inscripciones",
    "Lista las inscripciones del alumno. Lo usa el ALUMNO; no necesita datos.",
    {"ALUMNO"}, "lectura",
)
async def mis_inscripciones(token: str | None) -> dict:
    return resultado(await backend.llamar(
        "GET", "/students/enrollments", token, base_url=registro.BACKEND_URL))
