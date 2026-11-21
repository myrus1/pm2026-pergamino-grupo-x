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


def test_cargar_variables_vacias_usan_defaults(monkeypatch):
    for variable in ("LLM_BASE_URL", "LLM_MODEL", "LLM_API_KEY", "BACKEND_URL",
                     "ALLOWED_ORIGINS", "PORT"):
        monkeypatch.setenv(variable, "")
    monkeypatch.setenv("GROQ_API_KEY", "k")

    settings = cargar()

    assert settings.llm_base_url == "https://api.groq.com/openai/v1"
    assert settings.llm_model == "openai/gpt-oss-120b"
    assert settings.llm_api_key == "k"
    assert settings.backend_url == "http://localhost:8080"
    assert settings.allowed_origins == ("http://localhost:5173",)
    assert settings.port == 8000


def test_cargar_origenes_solo_comas_usa_default(monkeypatch):
    monkeypatch.setenv("ALLOWED_ORIGINS", " , ")
    monkeypatch.setenv("PORT", "  ")

    settings = cargar()

    assert settings.allowed_origins == ("http://localhost:5173",)
    assert settings.port == 8000


def test_cargar_respeta_valores(monkeypatch):
    monkeypatch.setenv("ALLOWED_ORIGINS", "https://a.com, https://b.com")
    monkeypatch.setenv("PORT", "9000")
    monkeypatch.setenv("LLM_API_KEY", "otra")
    monkeypatch.setenv("GROQ_API_KEY", "k")

    settings = cargar()

    assert settings.allowed_origins == ("https://a.com", "https://b.com")
    assert settings.port == 9000
    assert settings.llm_api_key == "otra"
