FROM python:3.12-slim

# uv, copiado de la imagen oficial (misma versión que se usa en desarrollo).
COPY --from=ghcr.io/astral-sh/uv:0.13.9 /uv /uvx /bin/

WORKDIR /app
ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PYTHONUNBUFFERED=1

# Primero las dependencias (se cachean mientras no cambien pyproject.toml y uv.lock).
COPY pyproject.toml uv.lock .python-version ./
RUN uv sync --frozen --no-dev --no-install-project

# Después el código del servicio.
COPY src ./src
RUN uv sync --frozen --no-dev

# Railway define PORT; en local queda 8000.
EXPOSE 8000

# IMPORTANTE: un solo worker. Las confirmaciones pendientes (borrados) viven en la
# memoria del proceso; con más de un worker, "confirmar" podría caer en otro proceso
# y no encontrar el pendiente. No agregar --workers.
CMD ["sh", "-c", "exec uv run --no-sync uvicorn asistente.app:app --host 0.0.0.0 --port ${PORT:-8000}"]
