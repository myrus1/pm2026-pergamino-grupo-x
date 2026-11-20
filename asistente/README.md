# Asistente por voz (servicio `asistente/`)

Servicio Python (FastAPI) que recibe un audio, lo transcribe, deja que un modelo de lenguaje elija
herramientas y las ejecuta contra el backend Spring Boot **con el token de quien habla**. Los permisos
los sigue decidiendo el backend: el asistente nunca usa un token propio.

- `GET /salud`: devuelve `{"estado": "ok"}`.
- `POST /voz`: multipart con `audio`, `historial`, `pendiente_id` y `confirmar`, más el header `Authorization`.
- `/mcp`: las mismas herramientas como servidor MCP (Streamable HTTP, sin estado). Lista todas las
  herramientas; si el rol no puede usar una, el backend responde 403 con el token del pedido.

Si faltan las claves, `/voz` responde `503` con "El asistente no está configurado.". Si Groq responde 429
(límite de uso), responde `503` con "El asistente está saturado, probá en un minuto.".

## Clave gratis de Groq

1. Entrá a [console.groq.com](https://console.groq.com) y creá una cuenta.
2. En **API Keys** creá una clave y copiala (no se vuelve a mostrar).
3. Guardala en `asistente/.env` como `GROQ_API_KEY`. Ese archivo no se sube al repositorio.

El plan gratuito tiene límites por minuto y por día. Si se superan, Groq responde 429: el asistente muestra el
mensaje de "saturado" y no cuesta nada; alcanza con esperar un rato.

## Variables

Copiá `.env.example` como `.env` dentro de `asistente/` y completá `GROQ_API_KEY`.

| Variable | Por defecto | Para qué sirve |
| --- | --- | --- |
| `GROQ_API_KEY` | (vacía) | Transcripción (`whisper-large-v3-turbo`, en español). Obligatoria. |
| `LLM_BASE_URL` | `https://api.groq.com/openai/v1` | API compatible con OpenAI del modelo que usa las herramientas. |
| `LLM_MODEL` | `openai/gpt-oss-120b` | Modelo. |
| `LLM_API_KEY` | `GROQ_API_KEY` | Clave del modelo; si falta se usa la de Groq. |
| `BACKEND_URL` | `http://localhost:8080` | Backend Spring Boot. |
| `ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes del front permitidos por CORS, separados por comas. |
| `PORT` | `8000` | Puerto (Railway lo define solo). |

## Correr en local

Requisitos: [uv](https://docs.astral.sh/uv/) (instala Python 3.12 si hace falta).

```bash
cd asistente
uv sync
set -a && . ./.env && set +a
uv run uvicorn asistente.app:app --port 8000
```

Los tests no usan red ni claves: `uv run pytest`.

> **Un solo worker.** Las confirmaciones pendientes (por ejemplo, borrar un alumno) viven en la memoria del
> proceso. No uses `--workers` con más de 1 ni escales a varias réplicas, o "confirmar" puede no encontrar el pendiente.

> **No escribir en Aiven en desarrollo.** La base del grupo es compartida. Para probar altas, bajas o
> cambios apuntá `BACKEND_URL` a un backend con una base local; contra la de Aiven usá solo consultas.

## Desplegar en Railway

1. En Railway: **New Project → Deploy from GitHub repo** y elegí este repositorio.
2. En **Settings → Source → Root Directory** poné `asistente/`. Railway usa el `asistente/Dockerfile`.
3. En **Variables** cargá `GROQ_API_KEY`, `BACKEND_URL` (la URL pública del backend) y `ALLOWED_ORIGINS`
   (la URL del front). El resto tiene valores por defecto. No cargues `PORT`: lo pone Railway.
4. En **Settings → Networking** generá un dominio público y comprobá `https://<dominio>/salud`.
5. En el front, apuntá la URL del asistente a ese dominio.

El `Dockerfile` parte de `python:3.12-slim`, instala `uv`, corre `uv sync --frozen --no-dev` y arranca
`uvicorn` con un solo worker en `${PORT:-8000}`. El build real lo hace Railway: en esta etapa no se probó con Docker.

## Usar otro modelo (Gemini o GitHub Models)

Solo cambian tres variables; la transcripción sigue siendo de Groq (`GROQ_API_KEY`). Los nombres de modelos
y las URLs cambian con el tiempo: **verificalos en la consola del proveedor** antes de usarlos.

| Proveedor | `LLM_BASE_URL` | `LLM_MODEL` | `LLM_API_KEY` |
| --- | --- | --- | --- |
| Gemini | `https://generativelanguage.googleapis.com/v1beta/openai/` | un modelo Gemini Flash (el nombre actual figura en Google AI Studio) | clave de Google AI Studio |
| GitHub Models | `https://models.github.ai/inference` | un modelo del catálogo de GitHub Models | token de GitHub con permiso para Models |

```env
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/
LLM_MODEL=<modelo Flash vigente>
LLM_API_KEY=<tu clave>
```

Los planes gratuitos de cada proveedor tienen sus propios límites. No todos los modelos soportan bien el uso
de herramientas: si el asistente no las llama, probá con otro.

## Conectarlo desde Claude Code (MCP)

1. Copiá `.mcp.json.example` (en la raíz del repositorio) como `.mcp.json`. Está en `.gitignore`.
2. Obtené un token (`POST /api/auth/token`) y exportalo: `export PM2026_TOKEN=<accessToken>`.
3. Con el asistente corriendo, abrí Claude Code en la raíz: aparece el servidor `pm2026` con las
   herramientas. Cada llamada usa tu token, así que ves y hacés solo lo que tu rol permite.

```json
{
  "mcpServers": {
    "pm2026": {
      "type": "http",
      "url": "http://localhost:8000/mcp",
      "headers": { "Authorization": "Bearer ${PM2026_TOKEN}" }
    }
  }
}
```

Para un servicio desplegado, cambiá la URL por `https://<dominio>/mcp`. Por el MCP las herramientas de borrado
no piden confirmación: la confirmación es parte del flujo de `/voz`.
