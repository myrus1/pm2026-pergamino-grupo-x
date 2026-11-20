import base64
import datetime
import json

import openai
import pytest
from fastapi.testclient import TestClient

from asistente.agente import PENDIENTES, AsistenteSaturado
from asistente.app import (
    crear_app, obtener_herramientas, obtener_modelo, obtener_transcriptor,
)
from asistente.config import Settings

CLAVES = {"transcripcion", "respuesta", "acciones", "datos", "deshacer", "pendiente", "secreto"}


class Transcriptor:
    def __init__(self, texto="hola", error=None):
        self.texto, self.error, self.llamadas = texto, error, []

    async def __call__(self, audio, nombre, tipo):
        self.llamadas.append((audio, nombre, tipo))
        if self.error:
            raise self.error
        return self.texto


class Modelo:
    def __init__(self):
        self.llamadas = 0

    async def responder(self, mensajes, herramientas):
        self.llamadas += 1
        self.mensajes = mensajes
        return {"role": "assistant", "content": "Hecho."}


class Herramientas:
    def __init__(self):
        self.llamadas = []

    async def llamar(self, nombre, args):
        self.llamadas.append((nombre, args))
        return {"ok": True}


@pytest.fixture
def ctx():
    PENDIENTES.clear()
    app = crear_app(Settings())
    t, m, h = Transcriptor(), Modelo(), Herramientas()
    app.dependency_overrides[obtener_transcriptor] = lambda: t
    app.dependency_overrides[obtener_modelo] = lambda: m
    app.dependency_overrides[obtener_herramientas] = lambda: h
    with TestClient(app) as c:
        yield c, t, m, h
    PENDIENTES.clear()


def audio(contenido=b"abc"):
    return {"audio": ("a.webm", contenido, "audio/webm")}


def test_voz_devuelve_contrato(ctx):
    c, t, m, h = ctx
    r = c.post("/voz", files=audio())
    assert r.status_code == 200
    assert set(r.json()) == CLAVES
    assert r.json()["transcripcion"] == "hola"
    assert r.json()["respuesta"] == "Hecho."
    assert t.llamadas == [(b"abc", "a.webm", "audio/webm")]


def test_audio_grande_400(ctx):
    c, t, m, h = ctx
    r = c.post("/voz", files=audio(b"0" * (10 * 1024 * 1024 + 1)))
    assert r.status_code == 400
    assert r.json() == {"detail": "El audio es demasiado largo."}
    assert t.llamadas == []


def test_silencio_no_llama_modelo(ctx):
    c, t, m, h = ctx
    t.texto = "   "
    r = c.post("/voz", files=audio())
    assert r.status_code == 200
    assert r.json()["respuesta"] == "No te entendí, probá de nuevo."
    assert r.json()["transcripcion"] == ""
    assert set(r.json()) == CLAVES
    assert m.llamadas == 0


def token_admin():
    claims = base64.urlsafe_b64encode(b'{"sub":"a@x.com","rol":"ADMINISTRADOR"}').rstrip(b"=")
    return f"Bearer eyJhbGciOiJIUzI1NiJ9.{claims.decode()}.firma"


def pendiente_de(token, nombre="borrar_alumno", args=None):
    from asistente.agente import _hash_token
    PENDIENTES["p1"] = (nombre, args or {"id": 5}, _hash_token(token),
                        datetime.datetime.now(datetime.UTC) + datetime.timedelta(minutes=5))


def test_si_por_voz_confirma(ctx):
    c, t, m, h = ctx
    tk = token_admin()
    pendiente_de(tk)
    t.texto = "Sí, dale"
    r = c.post("/voz", files=audio(), data={"pendiente_id": "p1"},
               headers={"Authorization": tk})
    assert r.status_code == 200
    assert h.llamadas == [("borrar_alumno", {"id": 5})]
    assert m.llamadas == 0
    assert r.json()["transcripcion"] == "Sí, dale"
    assert set(r.json()) == CLAVES


def test_confirmar_sin_audio(ctx):
    c, t, m, h = ctx
    tk = token_admin()
    pendiente_de(tk)
    r = c.post("/voz", data={"pendiente_id": "p1", "confirmar": "true"},
               headers={"Authorization": tk})
    assert r.status_code == 200
    assert t.llamadas == []
    assert h.llamadas == [("borrar_alumno", {"id": 5})]
    assert r.json()["transcripcion"] is None


def test_otra_cosa_descarta_pendiente(ctx):
    c, t, m, h = ctx
    tk = token_admin()
    pendiente_de(tk)
    t.texto = "mostrame los cursos"
    r = c.post("/voz", files=audio(), data={"pendiente_id": "p1"},
               headers={"Authorization": tk})
    assert r.status_code == 200
    assert "p1" not in PENDIENTES
    assert h.llamadas == []
    assert m.llamadas == 1


def test_sin_audio_ni_confirmar_400(ctx):
    c, *_ = ctx
    r = c.post("/voz", data={"pendiente_id": "x"})
    assert r.status_code == 400
    assert r.json() == {"detail": "Falta el audio."}


@pytest.mark.parametrize("valor", ["no es json", '{"a": 1}'])
def test_historial_invalido_400(ctx, valor):
    c, *_ = ctx
    r = c.post("/voz", files=audio(), data={"historial": valor})
    assert r.status_code == 400
    assert r.json() == {"detail": "historial inválido"}


def test_historial_se_recorta_a_6(ctx):
    c, t, m, h = ctx
    hist = [{"rol": "usuario", "texto": f"t{i}"} for i in range(10)]
    c.post("/voz", files=audio(), data={"historial": json.dumps(hist)})
    textos = [x["content"] for x in m.mensajes if x["role"] == "user"]
    assert textos == ["t4", "t5", "t6", "t7", "t8", "t9", "hola"]


def test_saturado_503(ctx):
    c, t, m, h = ctx
    t.error = AsistenteSaturado()
    r = c.post("/voz", files=audio())
    assert r.status_code == 503
    assert r.json() == {"detail": "El asistente está saturado, probá en un minuto."}


def test_cors_origen_permitido(ctx):
    c, *_ = ctx
    r = c.options("/voz", headers={
        "Origin": "http://localhost:5173",
        "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers": "authorization",
    })
    assert r.headers["access-control-allow-origin"] == "http://localhost:5173"


def test_cors_origen_no_permitido(ctx):
    c, *_ = ctx
    r = c.options("/voz", headers={
        "Origin": "http://malo.com", "Access-Control-Request-Method": "POST"})
    assert "access-control-allow-origin" not in r.headers


async def test_transcribir_groq(monkeypatch):
    from asistente import transcripcion
    capturado = {}

    class FakeTr:
        async def create(self, **kw):
            capturado.update(kw)
            if kw["file"][0] == "limite.webm":
                raise openai.RateLimitError(
                    "x", response=__import__("httpx").Response(
                        429, request=__import__("httpx").Request("POST", "http://x")),
                    body=None)

            class R:
                text = "  hola mundo \n"
            return R()

    class FakeCliente:
        def __init__(self, **kw):
            capturado["cliente"] = kw
            self.audio = type("A", (), {"transcriptions": FakeTr()})()

    monkeypatch.setattr(transcripcion.openai, "AsyncOpenAI", FakeCliente)
    s = Settings(groq_api_key="k")
    assert await transcripcion.transcribir(b"zz", "a.webm", "audio/webm", settings=s) == "hola mundo"
    assert capturado["model"] == "whisper-large-v3-turbo"
    assert capturado["language"] == "es"
    assert capturado["file"] == ("a.webm", b"zz", "audio/webm")
    assert capturado["cliente"]["base_url"] == "https://api.groq.com/openai/v1"
    with pytest.raises(AsistenteSaturado):
        await transcripcion.transcribir(b"zz", "limite.webm", "audio/webm", settings=s)
