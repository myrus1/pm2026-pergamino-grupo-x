import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    groq_api_key: str = ""
    llm_base_url: str = "https://api.groq.com/openai/v1"
    llm_model: str = "openai/gpt-oss-120b"
    llm_api_key: str = ""
    backend_url: str = "http://localhost:8080"
    allowed_origins: tuple[str, ...] = ("http://localhost:5173",)
    port: int = 8000


def _env(nombre: str, defecto: str) -> str:
    """Una variable vacía o con solo espacios (p. ej. `PORT=` en Railway) usa el valor por defecto."""
    return (os.environ.get(nombre) or "").strip() or defecto


def cargar() -> Settings:
    d = Settings()
    groq_api_key = _env("GROQ_API_KEY", d.groq_api_key)
    origenes = tuple(
        o.strip() for o in _env("ALLOWED_ORIGINS", "").split(",") if o.strip())
    return Settings(
        groq_api_key=groq_api_key,
        llm_base_url=_env("LLM_BASE_URL", d.llm_base_url),
        llm_model=_env("LLM_MODEL", d.llm_model),
        llm_api_key=_env("LLM_API_KEY", groq_api_key),
        backend_url=_env("BACKEND_URL", d.backend_url),
        allowed_origins=origenes or d.allowed_origins,
        port=int(_env("PORT", str(d.port))),
    )
