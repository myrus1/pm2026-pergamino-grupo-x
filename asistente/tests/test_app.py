from fastapi.testclient import TestClient

from asistente.app import crear_app
from asistente.config import cargar


def test_salud_responde_ok():
    cliente = TestClient(crear_app())

    respuesta = cliente.get("/salud")

    assert respuesta.status_code == 200
    assert respuesta.json() == {"estado": "ok"}


def test_cargar_usa_groq_por_defecto(monkeypatch):
    for variable in ("LLM_BASE_URL", "LLM_MODEL", "LLM_API_KEY", "BACKEND_URL", "ALLOWED_ORIGINS", "PORT"):
        monkeypatch.delenv(variable, raising=False)
    monkeypatch.setenv("GROQ_API_KEY", "k")

    settings = cargar()

    assert settings.llm_base_url == "https://api.groq.com/openai/v1"
    assert settings.llm_api_key == "k"
