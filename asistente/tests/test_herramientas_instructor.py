import base64
import json

import httpx
import respx

from asistente.herramientas import REGISTRO, registro

BASE = registro.BACKEND_URL


def _token(claims: dict) -> str:
    def b64(d: dict) -> str:
        return base64.urlsafe_b64encode(json.dumps(d).encode()).decode().rstrip("=")
    return f"{b64({'alg': 'HS512'})}.{b64(claims)}.firma"


@respx.mock
async def test_crear_curso_mapea_campos():
    import datetime
    ruta = respx.post(f"{BASE}/instructor/courses").mock(
        return_value=httpx.Response(201, json={"id": 9, "title": "Python"}))
    res = await REGISTRO["crear_curso"].funcion(
        "t", titulo="Python", descripcion=None, precio_base=100.5,
        fecha_inicio=datetime.date(2026, 12, 1), fecha_fin=datetime.date(2027, 1, 1),
        cupo=20, especialidad_id=3)
    body = json.loads(ruta.calls.last.request.content)
    assert body["title"] == "Python"
    assert body["basePrice"] == 100.5
    assert body["startDate"] == "2026-12-01"
    assert body["endDate"] == "2027-01-01"
    assert body["maxCapacity"] == 20
    assert body["specialtyId"] == 3
    assert res["ok"]
    d = REGISTRO["crear_curso"].deshacer({}, res)
    assert d["metodo"] == "DELETE" and d["ruta"] == "/courses/9" and d["descripcion"]


def test_crear_curso_deshacer_none_si_falla():
    assert REGISTRO["crear_curso"].deshacer({}, {"ok": False}) is None


@respx.mock
async def test_mis_cursos_filtra_por_token():
    respx.get(f"{BASE}/api/courses").mock(return_value=httpx.Response(200, json=[
        {"id": 1, "instructorId": 7}, {"id": 2, "instructorId": 8},
        {"id": 3, "instructorId": 7},
    ]))
    res = await REGISTRO["mis_cursos"].funcion(_token({"userId": 7}))
    assert res["ok"] and res["total"] == 2
    assert [c["id"] for c in res["datos"]] == [1, 3]


@respx.mock
async def test_mis_cursos_sin_userid_no_llama_al_backend():
    ruta = respx.get(f"{BASE}/api/courses").mock(return_value=httpx.Response(200, json=[]))
    res = await REGISTRO["mis_cursos"].funcion(None)
    assert res == {"ok": False, "status": 401, "code": None,
                   "mensaje": "Tu sesión venció o no iniciaste sesión."}
    assert not ruta.called


@respx.mock
async def test_editar_curso_solo_campos_dados():
    ruta = respx.put(f"{BASE}/instructor/courses/5").mock(
        return_value=httpx.Response(200, json={"id": 5}))
    await REGISTRO["editar_curso"].funcion("t", curso_id=5, titulo="X")
    assert json.loads(ruta.calls.last.request.content) == {"title": "X"}


def test_borrar_curso_es_borrado():
    assert REGISTRO["borrar_curso"].tipo == "borrado"


@respx.mock
async def test_lecturas_y_pedir_aprobacion_y_recaudacion():
    import datetime
    respx.get(f"{BASE}/instructor/courses/5").mock(return_value=httpx.Response(200, json={"id": 5}))
    respx.get(f"{BASE}/instructor/courses/5/students").mock(
        return_value=httpx.Response(200, json=[{"studentId": 1}]))
    ap = respx.post(f"{BASE}/instructor/courses/5/approval-requests").mock(
        return_value=httpx.Response(201, json={"id": 1}))
    respx.get(f"{BASE}/instructor/approval-requests").mock(
        return_value=httpx.Response(200, json=[]))
    rec = respx.get(f"{BASE}/instructor/revenue/enrollments").mock(
        return_value=httpx.Response(200, json=[]))
    assert (await REGISTRO["ver_mi_curso"].funcion("t", curso_id=5))["ok"]
    assert (await REGISTRO["alumnos_de_mi_curso"].funcion("t", curso_id=5))["total"] == 1
    await REGISTRO["pedir_aprobacion"].funcion("t", curso_id=5, comentario="listo")
    assert json.loads(ap.calls.last.request.content) == {"instructorComment": "listo"}
    assert (await REGISTRO["mis_solicitudes"].funcion("t"))["ok"]
    await REGISTRO["mi_recaudacion"].funcion(
        "t", desde=datetime.date(2026, 1, 1), hasta=None)
    q = rec.calls.last.request.url.params
    assert q["fromDate"] == "2026-01-01" and "toDate" not in q
