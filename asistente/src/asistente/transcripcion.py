"""Transcripción de audio con Groq Whisper. No se loguea el audio."""
import openai

from asistente.agente import AsistenteFallo, AsistenteNoConfigurado, AsistenteSaturado
from asistente.config import Settings

BASE_URL_GROQ = "https://api.groq.com/openai/v1"
MODELO = "whisper-large-v3-turbo"


async def transcribir(audio: bytes, nombre_archivo: str, tipo: str, *, settings: Settings) -> str:
    if not settings.groq_api_key:
        raise AsistenteNoConfigurado()
    cliente = openai.AsyncOpenAI(base_url=BASE_URL_GROQ, api_key=settings.groq_api_key)
    try:
        r = await cliente.audio.transcriptions.create(
            model=MODELO, language="es", file=(nombre_archivo, audio, tipo))
    except openai.RateLimitError as e:
        raise AsistenteSaturado() from e
    except openai.APIError as e:
        # Key inválida, audio muy corto o ilegible, red...: no se propaga como 500.
        raise AsistenteFallo() from e
    return (r.text or "").strip()
