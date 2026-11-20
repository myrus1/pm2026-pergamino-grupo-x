import datetime
import json

import httpx
import respx

from asistente.herramientas import REGISTRO, registro

BASE = registro.BACKEND_URL


@respx.mock
async def test_aprobar_manda_status():
    ruta = respx.put(f"{BASE}/admin/approval-requests/4").mock(
        return_value=httpx.Response(200, json={"id": 4, "status": "APPROVED"}))
    res = await REGISTRO["aprobar_solicitud"].funcion("t", 4, None)
    assert json.loads(ruta.calls.last.request.content) == {
        "status": "APPROVED", "adminComment": None}
    assert res["ok"]


@respx.mock
async def test_rechazar_manda_status_y_comentario():
    ruta = respx.put(f"{BASE}/admin/approval-requests/4").mock(
        return_value=httpx.Response(200, json={"id": 4}))
    await REGISTRO["rechazar_solicitud"].funcion("t", 4, "falta detalle")
    assert json.loads(ruta.calls.last.request.content) == {
        "status": "REJECTED", "adminComment": "falta detalle"}


@respx.mock
async def test_reporte_fechas_iso():
    ruta = respx.get(f"{BASE}/admin/reports/revenue").mock(
        return_value=httpx.Response(200, json={"total": 10}))
    res = await REGISTRO["reporte_recaudacion"].funcion(
        "t", datetime.date(2026, 10, 1), datetime.date(2026, 10, 31))
    q = ruta.calls.last.request.url.query.decode()
    assert "fromDate=2026-10-01" in q and "toDate=2026-10-31" in q
    assert res["ok"]


@respx.mock
async def test_reportes_rutas():
    for nombre, sufijo in [("reporte_recaudacion_por_curso", "courses-revenue"),
                           ("reporte_inscripciones", "enrollments")]:
        ruta = respx.get(f"{BASE}/admin/reports/{sufijo}").mock(
            return_value=httpx.Response(200, json=[]))
        await REGISTRO[nombre].funcion(
            "t", datetime.date(2026, 10, 1), datetime.date(2026, 10, 31))
        assert ruta.called


def test_todos_los_borrados_marcados():
    nombres = [n for n in REGISTRO if n.startswith(("borrar_", "quitar_"))]
    assert "borrar_alumno" in nombres and "quitar_especialidad" in nombres
    for n in nombres:
        assert REGISTRO[n].tipo == "borrado", n


def test_deshacer_rutas_relativas_a_api():
    ejemplos = {"id": 5, "nombre": "x"}
    cantidad = 0
    for h in REGISTRO.values():
        if h.deshacer is None:
            continue
        d = h.deshacer(
            {"instructor_id": 2, "especialidad_id": 3},
            {"ok": True, "datos": ejemplos})
        assert d is not None, h.nombre
        assert d["ruta"].split("/")[1] not in ("api", "admin"), h.nombre
        cantidad += 1
    assert cantidad >= 4


def test_solo_administrador():
    admin = [h for n, h in REGISTRO.items() if n in {
        "listar_alumnos", "crear_instructor", "aprobar_solicitud",
        "reporte_inscripciones", "borrar_administrador"}]
    assert len(admin) == 5
    assert all(h.roles == frozenset({"ADMINISTRADOR"}) for h in admin)


@respx.mock
async def test_crear_instructor_body_contrasena_y_deshacer():
    ruta = respx.post(f"{BASE}/api/instructors").mock(
        return_value=httpx.Response(201, json={"id": 12, "firstName": "Ana"}))
    res = await REGISTRO["crear_instructor"].funcion("t", "Ana", "Paz", "a@x.com")
    body = json.loads(ruta.calls.last.request.content)
    assert body["firstName"] == "Ana" and body["lastName"] == "Paz"
    assert body["email"] == "a@x.com" and len(body["password"]) == 10
    assert res["contrasena"] == body["password"]
    assert "contrasena" not in res["datos"]
    d = REGISTRO["crear_instructor"].deshacer({}, res)
    assert d["metodo"] == "DELETE" and d["ruta"] == "/instructors/12"


@respx.mock
async def test_crear_instructor_error_sin_contrasena():
    respx.post(f"{BASE}/api/instructors").mock(
        return_value=httpx.Response(409, json={"code": "EMAIL_DUP", "detail": "dup"}))
    res = await REGISTRO["crear_instructor"].funcion("t", "Ana", "Paz", "a@x.com")
    assert not res["ok"] and "contrasena" not in res
    assert REGISTRO["crear_instructor"].deshacer({}, res) is None


@respx.mock
async def test_crear_administrador_body_y_deshacer():
    ruta = respx.post(f"{BASE}/admin/accounts").mock(
        return_value=httpx.Response(201, json={"id": 3, "name": "Lu"}))
    res = await REGISTRO["crear_administrador"].funcion("t", "Lu", "Gomez", "l@x.com")
    body = json.loads(ruta.calls.last.request.content)
    assert body["name"] == "Lu" and body["lastName"] == "Gomez"
    assert body["email"] == "l@x.com" and res["contrasena"] == body["password"]
    d = REGISTRO["crear_administrador"].deshacer({}, res)
    assert d["ruta"] == "/administrators/3"


@respx.mock
async def test_asignar_especialidad_y_deshacer():
    ruta = respx.put(f"{BASE}/api/instructors/2/specialties/3").mock(
        return_value=httpx.Response(204))
    res = await REGISTRO["asignar_especialidad"].funcion("t", 2, 3)
    assert ruta.called and res["ok"]
    d = REGISTRO["asignar_especialidad"].deshacer(
        {"instructor_id": 2, "especialidad_id": 3}, res)
    assert d["metodo"] == "DELETE" and d["ruta"] == "/instructors/2/specialties/3"


@respx.mock
async def test_crear_especialidad_y_deshacer():
    ruta = respx.post(f"{BASE}/api/specialties").mock(
        return_value=httpx.Response(201, json={"id": 8, "name": "IA"}))
    res = await REGISTRO["crear_especialidad"].funcion("t", "IA")
    assert json.loads(ruta.calls.last.request.content) == {"name": "IA"}
    assert REGISTRO["crear_especialidad"].deshacer({}, res)["ruta"] == "/specialties/8"


@respx.mock
async def test_ver_alumno_dos_llamadas():
    respx.get(f"{BASE}/admin/students/5").mock(
        return_value=httpx.Response(200, json={"id": 5, "name": "Juan"}))
    respx.get(f"{BASE}/admin/students/5/courses").mock(
        return_value=httpx.Response(200, json=[{"id": 1}]))
    res = await REGISTRO["ver_alumno"].funcion("t", 5)
    assert res == {"ok": True, "datos": {"id": 5, "name": "Juan", "cursos": [{"id": 1}]}}


@respx.mock
async def test_ver_alumno_error_primero():
    respx.get(f"{BASE}/admin/students/5").mock(
        return_value=httpx.Response(404, json={"code": "NF", "detail": "no"}))
    res = await REGISTRO["ver_alumno"].funcion("t", 5)
    assert not res["ok"] and res["status"] == 404


@respx.mock
async def test_ver_curso_admin_incluye_alumnos():
    respx.get(f"{BASE}/admin/courses/7").mock(
        return_value=httpx.Response(200, json={"id": 7}))
    respx.get(f"{BASE}/admin/courses/7/students").mock(
        return_value=httpx.Response(200, json=[{"studentId": 1}]))
    res = await REGISTRO["ver_curso_admin"].funcion("t", 7)
    assert res["datos"] == {"id": 7, "alumnos": [{"studentId": 1}]}


@respx.mock
async def test_borrados_rutas():
    casos = [
        ("borrar_alumno", "/api/students/1", (1,)),
        ("borrar_instructor", "/api/instructors/1", (1,)),
        ("borrar_administrador", "/admin/accounts/1", (1,)),
        ("quitar_especialidad", "/api/instructors/1/specialties/2", (1, 2)),
        ("borrar_especialidad", "/api/specialties/1", (1,)),
    ]
    for nombre, ruta, args in casos:
        r = respx.delete(f"{BASE}{ruta}").mock(return_value=httpx.Response(204))
        res = await REGISTRO[nombre].funcion("t", *args)
        assert r.called and res["ok"], nombre


@respx.mock
async def test_listados_rutas():
    casos = {
        "listar_alumnos": "/admin/students", "listar_instructores": "/admin/instructors",
        "listar_administradores": "/admin/accounts",
        "solicitudes_pendientes": "/admin/approval-requests",
        "listar_cursos_admin": "/admin/courses",
    }
    for nombre, ruta in casos.items():
        r = respx.get(f"{BASE}{ruta}").mock(return_value=httpx.Response(200, json=[]))
        await REGISTRO[nombre].funcion("t")
        assert r.called, nombre
    r = respx.get(f"{BASE}/admin/instructors/4").mock(
        return_value=httpx.Response(200, json={"id": 4}))
    await REGISTRO["ver_instructor"].funcion("t", 4)
    assert r.called
