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


def cargar() -> Settings:
    groq_api_key = os.environ.get("GROQ_API_KEY", "")
    origenes = os.environ.get("ALLOWED_ORIGINS", "http://localhost:5173")
    return Settings(
        groq_api_key=groq_api_key,
        llm_base_url=os.environ.get("LLM_BASE_URL", "https://api.groq.com/openai/v1"),
        llm_model=os.environ.get("LLM_MODEL", "openai/gpt-oss-120b"),
        llm_api_key=os.environ.get("LLM_API_KEY") or groq_api_key,
        backend_url=os.environ.get("BACKEND_URL", "http://localhost:8080"),
        allowed_origins=tuple(o.strip() for o in origenes.split(",") if o.strip()),
        port=int(os.environ.get("PORT", "8000")),
    )
