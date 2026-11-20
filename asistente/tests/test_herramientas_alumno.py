import json
import re

import httpx
import respx

from asistente import herramientas
from asistente.backend import ResultadoApi
from asistente.herramientas import REGISTRO, para_rol, registro
from asistente.herramientas.registro import resultado

BASE = registro.BACKEND_URL


def _nombres(rol):
    return {h.nombre for h in para_rol(rol)}


def test_para_rol_alumno_no_ve_admin():
    nombres = _nombres("ALUMNO")
    assert "cursos_disponibles" in nombres
    assert "buscar_cursos" in nombres
    assert "aprobar_solicitud" not in nombres


def test_para_rol_none_solo_publicas():
    nombres = _nombres(None)
    assert {"quien_soy", "buscar_cursos", "listar_especialidades", "registrar_alumno",
            "registrar_instructor"} <= nombres
    assert "cursos_disponibles" not in nombres
    assert all("PUBLICO" in REGISTRO[n].roles for n in nombres)


@respx.mock
async def test_buscar_cursos_filtra_publicados():
    respx.get(f"{BASE}/api/courses").mock(return_value=httpx.Response(200, json=[
        {"id": 1, "title": "Python", "status": "DRAFT", "specialtyId": 1},
        {"id": 2, "title": "Python avanzado", "status": "PUBLISHED", "specialtyId": 1},
        {"id": 3, "title": "Java", "status": "PENDING_APPROVAL", "specialtyId": 2},
    ]))
    respx.get(f"{BASE}/api/specialties").mock(return_value=httpx.Response(200, json=[
        {"id": 1, "name": "Programación"}, {"id": 2, "name": "Backend"},
    ]))
    res = await REGISTRO["buscar_cursos"].funcion(None, texto="python", especialidad_id=1)
    assert res["ok"] and res["total"] == 1
    assert res["datos"][0]["id"] == 2
    assert res["datos"][0]["especialidad"] == "Programación"


@respx.mock
async def test_registrar_alumno_genera_contrasena():
    ruta = respx.post(f"{BASE}/students").mock(
        return_value=httpx.Response(201, json={"id": 57, "name": "Juan"}))
    res = await REGISTRO["registrar_alumno"].funcion(
        None, nombre="Juan", apellido="Pérez", dni="123", email="j@x.com")
    body = json.loads(ruta.calls.last.request.content)
    assert re.fullmatch(r"[A-Za-z0-9]{10}", body["password"])
    assert body["name"] == "Juan" and body["lastName"] == "Pérez"
    assert res["ok"] and res["contrasena"] == body["password"]


def test_registrar_alumno_deshacer():
    d = REGISTRO["registrar_alumno"].deshacer
    r = d({}, {"ok": True, "datos": {"id": 57}})
    assert r["metodo"] == "DELETE" and r["ruta"] == "/students/57" and r["descripcion"]
    assert d({}, {"ok": False}) is None


def test_resultado_recorta_listas():
    r = resultado(ResultadoApi(True, 200, list(range(25)), None, None))
    assert len(r["datos"]) == 20 and r["total"] == 25
    e = resultado(ResultadoApi(False, 409, None, "X", "dup"))
    assert e == {"ok": False, "status": 409, "code": "X", "mensaje": "dup"}


def test_quien_soy_y_claims():
    import base64
    p = base64.urlsafe_b64encode(json.dumps(
        {"sub": "a@x.com", "userId": 5, "rol": "ALUMNO"}).encode()).decode().rstrip("=")
    tok = f"Bearer h.{p}.s"
    assert registro.claims_del_token(tok)["rol"] == "ALUMNO"
    assert registro.claims_del_token("basura") is None
    assert registro.claims_del_token(None) is None
    assert herramientas.comunes  # importado
