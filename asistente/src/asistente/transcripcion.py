"""Transcripción de audio con Groq Whisper. No se loguea el audio."""
import time

import openai

from asistente import backend
from asistente.agente import AsistenteFallo, AsistenteNoConfigurado, AsistenteSaturado
from asistente.config import Settings

BASE_URL_GROQ = "https://api.groq.com/openai/v1"
# El modelo completo se equivoca menos que el turbo con nombres propios; tarda un poco más.
MODELO = "whisper-large-v3"

# Whisper usa el prompt como "texto anterior": con las palabras de la app las reconoce mejor.
VOCABULARIO = (
    "Plataforma de cursos. Curso, cursos, inscribime, inscripción, mis cursos, catálogo, "
    "instructor, instructora, administrador, alumno, especialidad, solicitud, solicitudes, "
    "aprobá, rechazá, recaudación, cupo, precio, fecha de inicio, borrá, creá, llevame a."
)
MAX_TITULOS = 25
MAX_PISTA = 700  # caracteres; Whisper toma ~224 tokens de prompt
VIGENCIA_TITULOS = 300  # segundos

_cache: tuple[float, list[str]] = (0.0, [])


async def _titulos_de_cursos(settings: Settings) -> list[str]:
    """Títulos de los cursos publicados, cacheados. Si el backend falla, no hay títulos."""
    global _cache
    if time.monotonic() - _cache[0] < VIGENCIA_TITULOS:
        return _cache[1]
    titulos: list[str] = []
    try:
        r = await backend.llamar("GET", "/api/courses", None, base_url=settings.backend_url)
        if r.ok and isinstance(r.datos, list):
            titulos = [c["title"] for c in r.datos
                       if isinstance(c, dict) and c.get("status") == "PUBLISHED"
                       and isinstance(c.get("title"), str)]
    except Exception:  # noqa: BLE001  la pista es opcional: nunca rompe la transcripción
        titulos = []
    _cache = (time.monotonic(), titulos[:MAX_TITULOS])
    return _cache[1]


async def pista(settings: Settings) -> str:
    titulos = await _titulos_de_cursos(settings)
    texto = VOCABULARIO + (" Cursos: " + ", ".join(titulos) + "." if titulos else "")
    return texto[:MAX_PISTA]


async def transcribir(audio: bytes, nombre_archivo: str, tipo: str, *, settings: Settings) -> str:
    if not settings.groq_api_key:
        raise AsistenteNoConfigurado()
    cliente = openai.AsyncOpenAI(base_url=BASE_URL_GROQ, api_key=settings.groq_api_key)
    try:
        r = await cliente.audio.transcriptions.create(
            model=MODELO, language="es", prompt=await pista(settings),
            file=(nombre_archivo, audio, tipo))
    except openai.RateLimitError as e:
        raise AsistenteSaturado() from e
    except openai.APIError as e:
        # Key inválida, audio muy corto o ilegible, red...: no se propaga como 500.
        raise AsistenteFallo() from e
    return (r.text or "").strip()
