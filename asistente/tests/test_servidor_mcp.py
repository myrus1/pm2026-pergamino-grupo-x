import json

import httpx
import httpx2
import respx
from mcp import Client
from mcp.client.streamable_http import streamable_http_client

from asistente import herramientas  # noqa: F401
from asistente.app import crear_app
from asistente.herramientas import REGISTRO, registro
from asistente.servidor_mcp import crear_mcp

BASE = registro.BACKEND_URL


async def test_lista_todas_las_herramientas():
    async with Client(crear_mcp()) as cliente:
        herramientas_mcp = (await cliente.list_tools()).tools

    nombres = {t.name for t in herramientas_mcp}
    assert len(herramientas_mcp) == len(REGISTRO)
    assert "cursos_disponibles" in nombres
    por_nombre = {t.name: t for t in herramientas_mcp}
    assert "token" not in por_nombre["ver_curso"].input_schema.get("properties", {})
    assert "curso_id" in por_nombre["ver_curso"].input_schema["properties"]
    assert por_nombre["cursos_disponibles"].annotations.read_only_hint is True


@respx.mock(assert_all_mocked=False)
async def test_llamada_reenvia_token(respx_mock):
    ruta = respx_mock.get(f"{BASE}/students/enrollments").mock(
        return_value=httpx.Response(200, json=[])
    )
    app = crear_app()
    transporte = httpx2.ASGITransport(app=app)
    async with app.router.lifespan_context(app):
        async with httpx2.AsyncClient(
            transport=transporte,
            base_url="http://testserver",
            headers={"Authorization": "Bearer t"},
        ) as http:
            async with Client(
                streamable_http_client("http://testserver/mcp", http_client=http)
            ) as cliente:
                resultado = await cliente.call_tool("mis_inscripciones", {})

    assert not resultado.is_error
    assert ruta.called
    assert ruta.calls.last.request.headers["authorization"] == "Bearer t"


async def test_ofrece_los_prompts():
    async with Client(crear_mcp()) as cliente:
        prompts = (await cliente.list_prompts()).prompts
        armado = await cliente.get_prompt("recomendar_curso", {"tema": "Python"})

    assert {p.name for p in prompts} == {
        "resumen_recaudacion", "recomendar_curso", "revisar_solicitudes"}
    texto = armado.messages[0].content.text
    assert "Python" in texto
    assert "buscar_cursos" in texto


@respx.mock(assert_all_mocked=False)
async def test_recurso_catalogo_solo_publicados(respx_mock):
    respx_mock.get(f"{BASE}/api/courses").mock(return_value=httpx.Response(200, json=[
        {"id": 1, "title": "Java", "basePrice": 100, "status": "PUBLISHED", "instructorId": 3},
        {"id": 2, "title": "Borrador secreto", "status": "DRAFT"},
    ]))
    async with Client(crear_mcp()) as cliente:
        recursos = (await cliente.list_resources()).resources
        leido = await cliente.read_resource("pm2026://cursos")

    assert [str(r.uri) for r in recursos] == ["pm2026://cursos"]
    cursos = json.loads(leido.contents[0].text)
    assert [c["title"] for c in cursos] == ["Java"]
    assert "instructorId" not in cursos[0]


@respx.mock(assert_all_mocked=False)
async def test_recurso_catalogo_backend_caido(respx_mock):
    respx_mock.get(f"{BASE}/api/courses").mock(return_value=httpx.Response(503))
    async with Client(crear_mcp()) as cliente:
        leido = await cliente.read_resource("pm2026://cursos")

    assert json.loads(leido.contents[0].text)["ok"] is False
