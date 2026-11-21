# Asistente por voz con MCP: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que un botón del front de PM2026 reciba un pedido dictado y lo ejecute en la plataforma, con los permisos del usuario logueado. Lo resuelve un servicio Python que transcribe, decide y actúa a través de un servidor MCP que envuelve la API REST.

**Architecture:**
- **Servicio `asistente/`.** Un solo proceso FastAPI en el repo del backend, con dos rutas montadas: `POST /voz` para el front y `/mcp`, el servidor FastMCP con transporte Streamable HTTP y sin sesión.
- **Agente.** Corre dentro de `/voz`. Transcribe con Groq Whisper y le pasa al modelo, por la API de chat compatible con OpenAI, solo las herramientas del rol del token. Las ejecuta como cliente MCP de su propio `/mcp`.
- **Front.** Agrega el botón de grabar y el panel de respuesta.

**Tech Stack:**
- Servicio: Python 3.12, `uv`, FastAPI, uvicorn, `mcp` (SDK oficial), `httpx`, `openai` (cliente compatible con Groq y Gemini).
- Tests del servicio: pytest, pytest-asyncio, respx.
- Front: React 19, TypeScript y Vite.

**Spec:** `docs/superpowers/specs/2026-11-20-asistente-voz-mcp-design.md`

**Dos repos:**
- Tareas 1 a 9: `myrus1/pm2026-pergamino-grupo-x`, rama `feat/asistente-voz` que sale de `dev`.
- Tareas 10 y 11: el fork del front, rama `feat/asistente-voz` que sale de `feature/30-adaptar-front-api`.
- Tarea 12: verificación.

Los commits son locales. No se hace push ni se abre un PR sin el OK de Alejandro. Ningún commit lleva líneas `Co-Authored-By` ni "Generated with".

## Global Constraints

- Python `>=3.12`, administrado con `uv` (`uv init`, `uv add`, `uv run`). La máquina tiene 3.9.6: `uv python install 3.12`.
- Variables de entorno:
  - `GROQ_API_KEY` (transcripción);
  - `LLM_BASE_URL` (por defecto `https://api.groq.com/openai/v1`);
  - `LLM_MODEL` (por defecto `openai/gpt-oss-120b`);
  - `LLM_API_KEY` (si falta, usa `GROQ_API_KEY`);
  - `BACKEND_URL` (por defecto `http://localhost:8080`);
  - `ALLOWED_ORIGINS` (lista separada por comas, por defecto `http://localhost:5173`);
  - `PORT` (por defecto `8000`).
- Transcripción: modelo `whisper-large-v3-turbo`, `language="es"`.
- Límites:
  - audio de 10 MB como máximo (si se pasa, 400);
  - 5 llamadas a herramientas por pedido;
  - listas de 20 elementos como máximo, con el total incluido;
  - "Deshacer" visible 10 segundos.
- Contraseña generada: 10 caracteres con `secrets` (letras y dígitos). Va solo en la respuesta y nunca al log ni al historial.
- Nombres de herramientas en castellano `snake_case`. Cada descripción dice qué rol la usa y qué datos pide.
- Solo se confirman las herramientas de tipo `borrado`. Las demás se ejecutan directo.
- Groq 429 (transcripción o modelo) → HTTP 503 con `{"detail": "El asistente está saturado, probá en un minuto."}`.
- El token nunca es uno de servicio: es el `Authorization` del pedido. El rol se lee del claim `rol` (`ALUMNO|INSTRUCTOR|ADMINISTRADOR`) sin verificar la firma.
- Tests sin red: Groq, el backend y el MCP se reemplazan por fakes o por respx.
- En el desarrollo no se escriben datos en Aiven. La prueba real de escritura (tarea 12) necesita el OK explícito de Alejandro.
- Front: no se agregan dependencias nuevas. Verificación con `npm run build` y `npm run lint`.

### Contrato `POST /voz` (lo comparten el servicio y el front)

Pedido `multipart/form-data`:
- `audio`: archivo; es opcional si viene `confirmar`.
- `historial`: JSON `[{"rol": "usuario"|"asistente", "texto": str}]`, con 6 turnos como máximo.
- `pendiente_id`: str, opcional.
- `confirmar`: `"true"`, opcional. Ejecuta el pendiente sin audio.
- Header `Authorization`: opcional.

Respuesta 200:

```json
{
  "transcripcion": "string|null",
  "respuesta": "string",
  "acciones": [{"herramienta": "string", "ok": true, "tipo": "lectura|escritura|borrado"}],
  "datos": {"filas": [{}], "total": 0} | null,
  "deshacer": {"metodo": "DELETE", "ruta": "/students/57", "descripcion": "string"} | null,
  "pendiente": {"id": "string", "descripcion": "string"} | null,
  "secreto": {"etiqueta": "Contraseña de Juan Pérez", "valor": "string"} | null
}
```

`deshacer.ruta` es relativa a `/api`: el front la manda con `apiFetch`, que le antepone `VITE_API_URL`.

## Review Focus

- **Pedido sin token** (por ejemplo, el registro desde la portada): solo se ven las herramientas públicas, y un pedido de administrador responde "necesitás iniciar sesión", no 500. Lo prueban las tareas 7 y 8.
- **Token vencido:** el backend da 401 y la respuesta es "tu sesión venció, volvé a entrar", sin crash. Lo prueban las tareas 2 y 7.
- **Audio vacío o silencio** (Whisper devuelve `""`): se responde "No te entendí, probá de nuevo" sin llamar al modelo. Lo prueba la tarea 8.
- **El modelo pide una herramienta que no existe o que no es de su rol:** el agente no la ejecuta y se lo devuelve al modelo como error. Lo prueba la tarea 7.
- **"Sí" dicho por voz con un pendiente vencido o ajeno:** el `pendiente_id` desconocido o vencido (5 minutos) no ejecuta nada y responde "Esa confirmación ya no vale". Lo prueba la tarea 7.

---

### Task 1: Proyecto `asistente/` con configuración y salud

**Files:**
- Create: `asistente/pyproject.toml`, `asistente/.python-version` (`3.12`), `asistente/src/asistente/__init__.py`, `asistente/src/asistente/config.py`, `asistente/src/asistente/app.py`, `asistente/tests/test_app.py`, `asistente/.gitignore` (`.venv/`, `__pycache__/`, `.env`)

**Interfaces:**
- Produces:
  - `config.Settings`: dataclass frozen con los campos de Global Constraints;
  - `config.cargar() -> Settings`: lee el entorno y aplica los defaults;
  - `app.crear_app(settings: Settings | None = None) -> FastAPI`;
  - `app.app = crear_app()`.

- [ ] **Step 1:** `cd asistente && uv init --package --python 3.12 && uv add fastapi "uvicorn[standard]" mcp httpx openai python-multipart && uv add --dev pytest pytest-asyncio respx`. Configurar `asyncio_mode = "auto"` en `[tool.pytest.ini_options]`.
- [ ] **Step 2: Confirmar la API del SDK `mcp` instalado y anotarla en un comentario al principio de `app.py`:**
  - el import de `FastMCP`;
  - el parámetro para modo sin sesión con respuesta JSON;
  - cómo se obtiene la app ASGI de Streamable HTTP y su ruta;
  - cómo una herramienta accede a los headers del pedido HTTP (vía `Context`).

  Si algún nombre difiere de lo que usan las tareas 6 y 7, esas tareas usan el nombre real.
- [ ] **Step 3: Escribir los tests que fallan:**
  - `test_salud_responde_ok`: `TestClient(crear_app()).get("/salud")` da 200 y `{"estado": "ok"}`;
  - `test_cargar_usa_groq_por_defecto`: con el entorno vacío salvo `GROQ_API_KEY=k`, `cargar().llm_base_url == "https://api.groq.com/openai/v1"` y `cargar().llm_api_key == "k"`.
- [ ] **Step 4:** `uv run pytest -q` → FAIL.
- [ ] **Step 5: Implementar `config.py` y `app.py`** (con `GET /salud`).
- [ ] **Step 6:** `uv run pytest -q` → PASS.
- [ ] **Step 7:** `git add asistente && git commit -m "feat(asistente): proyecto Python con configuracion y /salud"`

### Task 2: Cliente del backend

**Files:**
- Create: `asistente/src/asistente/backend.py`, `asistente/tests/test_backend.py`

**Interfaces:**
- Produces:
  - `@dataclass ResultadoApi(ok: bool, status: int, datos: Any, code: str | None, mensaje: str | None)`;
  - `async def llamar(metodo: str, ruta: str, token: str | None, *, json: dict | None = None, params: dict | None = None, base_url: str) -> ResultadoApi`:
    - antepone `base_url` a `ruta`;
    - manda `Authorization: Bearer <jwt>` (le saca un `Bearer ` repetido);
    - timeout de 15 s.

- [ ] **Step 1: Tests con respx:**
  - `test_reenvia_token`: el pedido sale con `Authorization: Bearer abc` aunque `token="Bearer abc"`;
  - `test_problem_detail_a_resultado`: un 403 con `{"code":"authorization.denied","detail":"No tiene permisos"}` da `ok=False, status=403, code="authorization.denied", mensaje="No tiene permisos"`;
  - `test_204_sin_cuerpo`: da `ok=True, datos=None`;
  - `test_backend_caido`: `httpx.ConnectError` da `ok=False, status=0, mensaje="No se pudo conectar con el backend"`;
  - `test_401_mensaje_sesion`: un 401 da `mensaje="Tu sesión venció o no iniciaste sesión."`.
- [ ] **Step 2:** `uv run pytest tests/test_backend.py -q` → FAIL.
- [ ] **Step 3: Implementar `llamar`.**
- [ ] **Step 4:** PASS.
- [ ] **Step 5:** commit `feat(asistente): cliente HTTP del backend`.

### Task 3: Registro de herramientas, comunes y de alumno

**Files:**
- Create: `asistente/src/asistente/herramientas/__init__.py`, `registro.py`, `comunes.py`, `alumno.py`, `asistente/tests/test_herramientas_alumno.py`

**Interfaces:**
- Consumes: `backend.llamar`, `ResultadoApi`.
- Produces:
  - `Rol = Literal["PUBLICO","ALUMNO","INSTRUCTOR","ADMINISTRADOR"]`;
  - `Tipo = Literal["lectura","escritura","borrado"]`;
  - `@dataclass Herramienta(nombre: str, descripcion: str, roles: frozenset[Rol], tipo: Tipo, funcion: Callable[..., Awaitable[dict]], deshacer: Callable[[dict, dict], dict | None] | None = None)`. `deshacer(args, resultado)` devuelve `{"metodo","ruta","descripcion"}`.
  - `REGISTRO: dict[str, Herramienta]`;
  - decorador `herramienta(nombre, descripcion, roles, tipo, deshacer=None)`, que registra la función. La función recibe `token: str | None` como primer parámetro y después los argumentos de la herramienta, con type hints, que son el esquema.
  - `def para_rol(rol: Rol | None) -> list[Herramienta]`: devuelve las herramientas con `"PUBLICO"` más las de su rol.
  - `def resultado(r: ResultadoApi, limite: int = 20) -> dict`: da `{"ok": True, "datos": ..., "total": n}` y, si es una lista, la recorta a `limite`; si no, `{"ok": False, "status", "code", "mensaje"}`.
  - `BACKEND_URL` sale de `config.cargar()` una sola vez.

Herramientas de esta tarea (nombre → endpoint):
- **Comunes:**
  - `quien_soy` (PUBLICO, lee los claims sin red: `{rol, userId, email}`, o "sin sesión");
  - `buscar_cursos(texto: str | None, especialidad_id: int | None)` → `GET /api/courses`. Filtra `status == "PUBLISHED"`, texto en el título y la especialidad, y le agrega `especialidad` (nombre) desde `GET /api/specialties`;
  - `listar_especialidades` → `GET /api/specialties`;
  - `registrar_alumno(nombre, apellido, dni, email)` (PUBLICO + ADMINISTRADOR, escritura) → `POST /students` con la contraseña generada, que viene en el resultado como `contrasena`. `deshacer` → `DELETE /students/{id}`;
  - `registrar_instructor(nombre, apellido, email)` (PUBLICO, escritura) → `POST /instructor`, con la contraseña generada.
- **Alumno:**
  - `cursos_disponibles` → `GET /student/courses/available`;
  - `ver_curso(curso_id)` → `GET /student/courses/{id}`;
  - `inscribirme(curso_id)` (escritura) → `POST /student/enrollments`;
  - `mis_inscripciones` → `GET /students/enrollments`.

- [ ] **Step 1: Tests:**
  - `test_para_rol_alumno_no_ve_admin`: los nombres de `para_rol("ALUMNO")` incluyen `cursos_disponibles` y `buscar_cursos` y no incluyen `aprobar_solicitud`. Es un nombre de la tarea 5, así que este test se escribe con `assert "aprobar_solicitud" not in ...`, que vale también antes de que exista.
  - `test_para_rol_none_solo_publicas`.
  - `test_buscar_cursos_filtra_publicados`: con respx, de 3 cursos (DRAFT, PUBLISHED, PENDING_APPROVAL) devuelve 1, con `especialidad` resuelta.
  - `test_registrar_alumno_genera_contrasena`: el body enviado tiene `password` de 10 caracteres alfanuméricos y el resultado trae `contrasena` igual.
  - `test_registrar_alumno_deshacer`: con resultado `{"datos": {"id": 57}}` da `{"metodo": "DELETE", "ruta": "/students/57", ...}`.
  - `test_resultado_recorta_listas`: 25 elementos dan 20 y `total == 25`.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4:** PASS.
- [ ] **Step 5:** commit `feat(asistente): herramientas comunes y de alumno`.

### Task 4: Herramientas de instructor

**Files:**
- Create: `asistente/src/asistente/herramientas/instructor.py`, `asistente/tests/test_herramientas_instructor.py`

**Interfaces:** consume el decorador `herramienta` y `resultado`, y registra en `REGISTRO`.

Herramientas (rol INSTRUCTOR):
- `mis_cursos` → `GET /api/courses` filtrado por `instructorId == userId` del token;
- `ver_mi_curso(curso_id)` → `GET /instructor/courses/{id}`;
- `alumnos_de_mi_curso(curso_id)` → `GET /instructor/courses/{id}/students`;
- `crear_curso(titulo, descripcion: str | None, precio_base: float, fecha_inicio: date, fecha_fin: date, cupo: int, especialidad_id: int)` (escritura) → `POST /instructor/courses`. `deshacer` → `DELETE /courses/{id}`;
- `editar_curso(curso_id, campos opcionales)` (escritura) → `PUT /instructor/courses/{id}`, solo con los campos dados;
- `borrar_curso(curso_id)` (borrado) → `DELETE /instructor/courses/{id}`;
- `pedir_aprobacion(curso_id, comentario: str | None)` (escritura) → `POST /instructor/courses/{id}/approval-requests`;
- `mis_solicitudes` → `GET /instructor/approval-requests`;
- `mi_recaudacion(desde: date | None, hasta: date | None)` → `GET /instructor/revenue/enrollments`.

El body usa los nombres de la API: `title`, `description`, `basePrice`, `startDate`, `endDate`, `maxCapacity`, `specialtyId`.

- [ ] **Step 1: Tests:**
  - `test_crear_curso_mapea_campos`: el body enviado tiene `basePrice`, `startDate="2026-12-01"` y `specialtyId`;
  - `test_mis_cursos_filtra_por_token`: un token con `userId=7` devuelve solo los cursos con `instructorId == 7`;
  - `test_editar_curso_solo_campos_dados`: con `titulo="X"`, el body es exactamente `{"title": "X"}`;
  - `test_borrar_curso_es_borrado`: `REGISTRO["borrar_curso"].tipo == "borrado"`.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4:** PASS.
- [ ] **Step 5:** commit `feat(asistente): herramientas de instructor`.

### Task 5: Herramientas de administrador

**Files:**
- Create: `asistente/src/asistente/herramientas/admin.py`, `asistente/tests/test_herramientas_admin.py`

Herramientas (rol ADMINISTRADOR). Se usa la ruta del TP6 si existe y si no la de `/api`.
- **Usuarios:**
  - `listar_alumnos` → `GET /admin/students`;
  - `ver_alumno(id)` → `GET /admin/students/{id}` más `/courses`;
  - `borrar_alumno(id)` (borrado) → `DELETE /api/students/{id}`;
  - `listar_instructores` → `GET /admin/instructors`;
  - `ver_instructor(id)` → `GET /admin/instructors/{id}`;
  - `crear_instructor(nombre, apellido, email)` (escritura, con contraseña generada) → `POST /api/instructors`. `deshacer` → `DELETE /instructors/{id}`;
  - `borrar_instructor(id)` (borrado) → `DELETE /api/instructors/{id}`;
  - `listar_administradores` → `GET /admin/accounts`;
  - `crear_administrador(nombre, apellido, email)` (escritura, con contraseña generada) → `POST /admin/accounts`. `deshacer` → `DELETE /administrators/{id}`;
  - `borrar_administrador(id)` (borrado) → `DELETE /admin/accounts/{id}`.
- **Especialidades:**
  - `asignar_especialidad(instructor_id, especialidad_id)` (escritura) → `PUT /api/instructors/{id}/specialties/{sid}`. `deshacer` → `DELETE` de la misma ruta sin `/api`;
  - `quitar_especialidad(...)` (borrado);
  - `crear_especialidad(nombre)` (escritura) → `POST /api/specialties`. `deshacer` → `DELETE /specialties/{id}`;
  - `borrar_especialidad(id)` (borrado).
- **Solicitudes:**
  - `solicitudes_pendientes` → `GET /admin/approval-requests`;
  - `aprobar_solicitud(id, comentario: str | None)` y `rechazar_solicitud(...)` (escritura) → `PUT /admin/approval-requests/{id}` con `{status, adminComment}`.
- **Cursos:** `listar_cursos_admin` → `GET /admin/courses`; `ver_curso_admin(id)` → `GET /admin/courses/{id}` más `/students`.
- **Reportes** (`desde` y `hasta` obligatorios, `date`):
  - `reporte_recaudacion` → `GET /admin/reports/revenue`;
  - `reporte_recaudacion_por_curso` → `/courses-revenue`;
  - `reporte_inscripciones` → `/enrollments`.

  Los parámetros que se mandan son `fromDate` y `toDate` en formato `YYYY-MM-DD`.

- [ ] **Step 1: Tests:**
  - `test_aprobar_manda_status`: el body es `{"status": "APPROVED", "adminComment": None}`;
  - `test_reporte_fechas_iso`: los params son `fromDate=2026-10-01&toDate=2026-10-31`;
  - `test_todos_los_borrados_marcados`: todo nombre que empieza con `borrar_` o `quitar_` tiene `tipo == "borrado"`;
  - `test_deshacer_rutas_relativas_a_api`: ningún `deshacer` de `REGISTRO` devuelve una ruta que empiece con `/api` ni con `/admin`.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4:** PASS.
- [ ] **Step 5:** commit `feat(asistente): herramientas de administrador`.

### Task 6: Servidor MCP en `/mcp`

**Files:**
- Create: `asistente/src/asistente/servidor_mcp.py`, `asistente/tests/test_servidor_mcp.py`
- Modify: `asistente/src/asistente/app.py`. Monta el MCP en `/mcp` y corre su `session_manager` en el lifespan de FastAPI.

**Interfaces:**
- Consumes: `REGISTRO`.
- Produces:
  - `def crear_mcp() -> FastMCP`, con nombre `pm2026` y sin sesión. Registra cada `Herramienta` de `REGISTRO` con su nombre, su descripción y `readOnlyHint = (tipo == "lectura")` / `destructiveHint = (tipo == "borrado")`. El esquema sale de los argumentos sin `token`.
  - `def token_del_pedido(ctx) -> str | None`: lee `Authorization` del pedido HTTP actual.
  - El MCP **no** filtra por rol: lista todas las herramientas. Los permisos los controla el backend. El filtro por rol es del agente (tarea 7).

- [ ] **Step 1: Tests:**
  - `test_lista_todas_las_herramientas`: con un cliente MCP en memoria contra `crear_mcp()` (el helper de pruebas del SDK), `tools/list` devuelve `len(REGISTRO)` herramientas, entre ellas `cursos_disponibles`;
  - `test_llamada_reenvia_token`: con respx sobre el backend, llamar a `mis_inscripciones` por HTTP a la app con `Authorization: Bearer t` deja al backend recibiendo `Bearer t`. Usa `httpx.AsyncClient(transport=ASGITransport(app))` y el cliente Streamable HTTP del SDK con ese cliente.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4:** PASS.
- [ ] **Step 5: Prueba manual.** Con `uv run uvicorn asistente.app:app --port 8000` y el backend local levantado, correr `npx @modelcontextprotocol/inspector@2.13.0 --cli http://localhost:8000/mcp --method tools/list`. Tiene que listar las herramientas.
- [ ] **Step 6:** commit `feat(asistente): servidor MCP en /mcp`.

### Task 7: Agente

**Files:**
- Create: `asistente/src/asistente/agente.py`, `asistente/src/asistente/cliente_mcp.py`, `asistente/tests/test_agente.py`

**Interfaces:**
- Consumes: `REGISTRO` y `para_rol` (para los metadatos y el filtro).
- Produces:
  - `class ClienteHerramientas(Protocol)`: `async def llamar(self, nombre: str, args: dict) -> dict`.
  - `class ClienteMcpHttp`: implementa `llamar`, conectando al `/mcp` propio (`http://127.0.0.1:{PORT}/mcp`) con el `Authorization` del usuario.
  - `class Modelo(Protocol)`: `async def responder(self, mensajes: list[dict], herramientas: list[dict]) -> dict`. Devuelve el mensaje del asistente en formato chat de OpenAI, con `content` y/o `tool_calls`.
  - `class ModeloOpenAI`: implementa `Modelo` con `openai.AsyncOpenAI(base_url=LLM_BASE_URL, api_key=LLM_API_KEY)`.
  - `class AsistenteSaturado(Exception)`: el modelo o la transcripción devolvieron 429.
  - `def rol_del_token(token: str | None) -> Rol | None`: decodifica el payload base64url, mapea el claim `rol` a `ALUMNO|INSTRUCTOR|ADMINISTRADOR` y da `None` si no hay token o no se puede leer.
  - `async def ejecutar(texto: str, token: str | None, historial: list[dict], *, modelo: Modelo, herramientas: ClienteHerramientas, hoy: date) -> dict`: devuelve el contrato `POST /voz` sin `transcripcion`.
  - `async def confirmar(pendiente_id: str, token: str | None, herramientas: ClienteHerramientas) -> dict`.
  - `PENDIENTES`: dict en memoria `id -> (herramienta, args, sha256(token), vence)`, con un vencimiento de 5 minutos.

Algoritmo de `ejecutar` (el que la firma no determina):
1. Mensajes: un system prompt y luego `historial` y `texto`.
   - El system prompt dice:
     - que responda en castellano, breve;
     - la fecha `hoy` en ISO y el rol;
     - que no invente datos: si falta uno, que lo pida;
     - que no muestre contraseñas: el sistema las muestra.
   - Las herramientas son `para_rol(rol)` en formato `tools` de OpenAI.
2. Hasta 5 vueltas. En cada una:
   - si el modelo no pide herramientas, se termina y `respuesta = content`;
   - por cada `tool_call`:
     - si no está en `para_rol(rol)`, se agrega un resultado de error "herramienta no disponible para tu rol";
     - si es `borrado`, se guarda en `PENDIENTES`, se corta el loop y se devuelve `pendiente` con la descripción "¿Confirmás <nombre legible> con <args>?";
     - si es cualquier otra, `await herramientas.llamar(...)`. Se agrega a `acciones`. Si es una lista, se guarda como `datos`. Si la herramienta tiene `deshacer` y salió bien, se calcula `deshacer` (solo si el rol del token puede ejecutar el borrado inverso: para `registrar_alumno` e instructores, solo ADMINISTRADOR; el instructor puede deshacer `crear_curso`). Si el resultado trae `contrasena`, se mueve a `secreto` y se borra del texto que ve el modelo.
3. Al llegar a 5 vueltas, la respuesta es "Hice lo que pude; el pedido era muy largo." con las acciones hechas.

- [ ] **Step 1: Tests con fakes de `Modelo` (respuestas guionadas) y de `ClienteHerramientas`:**
  - `test_filtra_herramientas_por_rol`: con token ALUMNO, el `herramientas` que recibe el modelo no incluye `listar_alumnos`;
  - `test_rechaza_herramienta_de_otro_rol`: si el modelo pide `listar_alumnos` con token ALUMNO, el cliente no se llama y el siguiente mensaje al modelo contiene "no disponible";
  - `test_borrado_queda_pendiente`: si el modelo pide `borrar_alumno(id=5)`, el cliente no se llama, `pendiente.id` existe y `PENDIENTES` tiene la entrada;
  - `test_confirmar_ejecuta_pendiente`: `confirmar(id, mismo_token)` llama a `borrar_alumno` con `{"id": 5}`;
  - `test_confirmar_otro_token_no_ejecuta`: con otro token responde "Esa confirmación ya no vale";
  - `test_confirmar_vencido_no_ejecuta`: con `vence` en el pasado, mismo mensaje;
  - `test_limite_cinco_llamadas`: un modelo que siempre pide `listar_especialidades` produce 5 acciones y el mensaje de corte;
  - `test_contrasena_va_a_secreto`: el resultado de `registrar_alumno` con `contrasena` deja `secreto.valor` igual a ella y ningún mensaje enviado al modelo contiene la contraseña;
  - `test_sin_token_rol_none`: `rol_del_token(None) is None` y solo se ofrecen las herramientas públicas;
  - `test_rol_del_token_mapea_claims`: un JWT de prueba con `{"rol": "INSTRUCTOR"}` da `"INSTRUCTOR"`.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3: Implementar.** `ModeloOpenAI` convierte `openai.RateLimitError` en `AsistenteSaturado`.
- [ ] **Step 4:** PASS.
- [ ] **Step 5:** commit `feat(asistente): agente con filtro por rol y confirmacion de borrados`.

### Task 8: Transcripción y `POST /voz`

**Files:**
- Create: `asistente/src/asistente/transcripcion.py`, `asistente/tests/test_voz.py`
- Modify: `asistente/src/asistente/app.py`. Agrega `POST /voz` y `CORSMiddleware` con `ALLOWED_ORIGINS`, métodos `POST`, headers `Authorization` y `Content-Type`.

**Interfaces:**
- Consumes: `agente.ejecutar`, `agente.confirmar`, `AsistenteSaturado`, `ClienteMcpHttp`, `ModeloOpenAI`.
- Produces:
  - `async def transcribir(audio: bytes, nombre_archivo: str, tipo: str) -> str`: llama a Groq `audio.transcriptions.create` con `whisper-large-v3-turbo` y `language="es"`. Un 429 se convierte en `AsistenteSaturado`.
  - Dependencias inyectables de FastAPI: `obtener_transcriptor`, `obtener_modelo` y `obtener_herramientas`, para reemplazarlas en los tests.

Comportamiento de `/voz`:
- con `confirmar=true` y `pendiente_id`, llama a `agente.confirmar` y no transcribe;
- si el audio pesa más de 10 MB, responde 400 `{"detail": "El audio es demasiado largo."}`;
- si la transcripción queda vacía o en blanco, responde 200 con `respuesta = "No te entendí, probá de nuevo."` y no llama al modelo;
- un `pendiente_id` con audio es la respuesta por voz a un pendiente:
  - si la transcripción normalizada empieza con `sí`, `si`, `dale`, `confirmo` o `borralo`, llama a `confirmar`;
  - si no, se descarta el pendiente y se sigue con `ejecutar`.
- `AsistenteSaturado` → 503 con el mensaje de Global Constraints.

- [ ] **Step 1: Tests con `TestClient` y dependencias reemplazadas:**
  - `test_voz_devuelve_contrato`: las claves de la respuesta son exactamente las del contrato;
  - `test_audio_grande_400`;
  - `test_silencio_no_llama_modelo`;
  - `test_si_por_voz_confirma`: la transcripción "Sí, dale" con `pendiente_id` llama a `confirmar`;
  - `test_otra_cosa_descarta_pendiente`;
  - `test_saturado_503`;
  - `test_cors_origen_permitido`: un preflight desde `http://localhost:5173` devuelve `access-control-allow-origin` con ese origen.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4:** `uv run pytest -q` (toda la suite) → PASS.
- [ ] **Step 5:** commit `feat(asistente): POST /voz con transcripcion de Groq`.

### Task 9: Despliegue y documentación del servicio

**Files:**
- Create: `asistente/Dockerfile`, `asistente/README.md`, `asistente/.env.example` (solo nombres de variables, sin valores reales), `.mcp.json.example` en la raíz.
- Modify: `README.md` de la raíz (sección "Asistente por voz (extra)") y `.dockerignore`/`.gitignore` de la raíz (`asistente/.venv`, `**/.env`).

Contenido:
- **Dockerfile:** parte de `python:3.12-slim`, instala `uv`, corre `uv sync --frozen --no-dev` y arranca con `uv run uvicorn asistente.app:app --host 0.0.0.0 --port ${PORT:-8000}`.
- **README de `asistente/`:**
  - cómo crear la clave gratis de Groq;
  - las variables;
  - cómo correrlo local (`uv run uvicorn ...`);
  - cómo crear el servicio en Railway con root `asistente/`;
  - cómo cambiar a Gemini o GitHub Models (`LLM_BASE_URL`, `LLM_MODEL`, `LLM_API_KEY`);
  - cómo conectarlo desde Claude Code con `.mcp.json`;
  - el aviso de que en desarrollo no se escribe en Aiven.
- **`.mcp.json.example`:** servidor `pm2026` de tipo `http` a `http://localhost:8000/mcp` con `"headers": {"Authorization": "Bearer ${PM2026_TOKEN}"}`.

- [ ] **Step 1:** `docker` no está instalado en esta máquina. Verificar el Dockerfile con una lectura cuidadosa y con `uv sync --frozen --no-dev` local. Avisar a Facu que el build real lo hace Railway.
- [ ] **Step 2:** `cd backend && ./mvnw -q -o test -Dtest='!ApiDocumentationTest,!ObservabilityIntegrationTest,!Pm2026ApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false` → PASS. Es para confirmar que el backend no cambió.
- [ ] **Step 3:** commit `docs(asistente): despliegue en Railway y README`.

### Task 10: Front, grabador y cliente del asistente

Repo del front, rama `feat/asistente-voz` que sale de `feature/30-adaptar-front-api`.

**Files:**
- Create:
  - `src/voice/useAudioRecorder.ts`: port a TS de `G-Comunicaciones/src/hooks/useAudioRecorder.js`, sin la reproducción ni la vista previa;
  - `src/api/asistente.ts`;
  - `src/types/asistente.ts`.
- Modify: `src/vite-env.d.ts` (`VITE_ASISTENTE_URL?: string`).

**Interfaces:**
- Produces:
  - `useAudioRecorder(): { estado: 'inactivo'|'grabando'|'error', error: string | null, empezar(): Promise<void>, terminar(): Promise<Blob | null> }`. Graba con `MediaRecorder`, en `audio/webm` o `audio/mp4` según soporte, y corta solo a los 60 segundos.
  - `types/asistente.ts`: `RespuestaVoz`, `TurnoHistorial` y `AccionDeshacer`, que reflejan exactamente el contrato de `POST /voz`.
  - `asistenteDisponible(): boolean`: indica si está definida `VITE_ASISTENTE_URL`.
  - `enviarVoz(params: { audio?: Blob, historial: TurnoHistorial[], pendienteId?: string, confirmar?: boolean }): Promise<RespuestaVoz>`: `fetch` a `${VITE_ASISTENTE_URL}/voz` con el token de la sesión (la misma lectura que `readSessionToken` de `client.ts`; si hace falta, se exporta). Un 503 o un 400 lanza `ApiError` con el `detail`.

- [ ] **Step 1: Implementar los tres archivos.**
- [ ] **Step 2:** `npm run build && npm run lint` → sin errores.
- [ ] **Step 3:** commit `feat(asistente): grabador de audio y cliente de /voz`.

### Task 11: Front, botón y panel

**Files:**
- Create: `src/voice/BotonVoz.tsx`, `src/voice/PanelVoz.tsx`, `src/voice/useAsistente.ts`
- Modify: `src/components/Layout.tsx` (monta `<BotonVoz />` y pone `key={refrescos}` en el `<Outlet />`), la portada en `src/pages/PublicPages.tsx` (monta `<BotonVoz />`) y `src/index.css` (estilos con las variables existentes).

**Interfaces:**
- Consumes: `useAudioRecorder`, `enviarVoz`, `apiFetch` (para "Deshacer") y `useToast`.
- Produces:
  - `useAsistente({ alCambiar }: { alCambiar(): void })`: maneja el historial en memoria (6 turnos como máximo), la última respuesta, el pendiente y el estado `'listo'|'escuchando'|'pensando'`;
  - `BotonVoz`: no se renderiza si `!asistenteDisponible()`.

Comportamiento:
- **Botón:** con `pointerdown` empieza a grabar y con `pointerup`/`pointerleave` termina y envía. Con teclado: Espacio o Enter alternan entre empezar y terminar. Tiene `aria-pressed` mientras graba y `aria-live="polite"` en el panel.
- **El panel muestra:**
  - la transcripción;
  - la respuesta;
  - `datos.filas` como tabla, con las columnas de la primera fila y "y N más" si `total > filas.length`;
  - `secreto`, con un botón Copiar y el aviso "se muestra una sola vez";
  - `pendiente`, con Sí y No (Sí llama a `enviarVoz({ pendienteId, confirmar: true })`);
  - `deshacer`, con un botón que desaparece a los 10 segundos y llama a `apiFetch(ruta, { method: metodo })`, seguido de un toast.
- **Refresco:** si alguna acción tiene `ok` y `tipo !== 'lectura'`, o después de confirmar o deshacer, se llama a `alCambiar`. Eso incrementa `refrescos` en `Layout` y vuelve a montar la página.
- **Errores:** se muestran en el panel, sin modal.
- **Diseño:** sigue `src/index.css` (colores, radios y tipografía existentes), sin estilos nuevos de otra paleta.

- [ ] **Step 1: Implementar.**
- [ ] **Step 2:** `npm run build && npm run lint` → sin errores.
- [ ] **Step 3: Prueba manual en el navegador.**
  - Levantar el backend local, `uv run uvicorn asistente.app:app --port 8000` y `VITE_ASISTENTE_URL=http://localhost:8000 npm run dev`.
  - Con una sesión, el botón aparece; sin `VITE_ASISTENTE_URL`, no.
  - Grabar "listá las especialidades" muestra la tabla. Es solo lectura.
- [ ] **Step 4:** commit `feat(asistente): boton de voz y panel de respuesta`.

### Task 12: Verificación de punta a punta

- [ ] **Step 1: Lecturas por voz contra el backend local** (lee Aiven, no escribe), con una cuenta de cada rol que ya exista:
  - alumno: "¿en qué cursos me puedo inscribir?";
  - instructor: "¿cuántos inscriptos tiene mi curso X?";
  - administrador: "¿cuánto se recaudó en octubre?".
- [ ] **Step 2: Permisos.** Con el token de alumno, "listá todos los alumnos" responde que no está disponible para su rol.
- [ ] **Step 3: Pedir el OK de Alejandro para escribir en Aiven.** Recién con el OK: "registrá a <nombre de prueba>…", ver la contraseña, "Deshacer", y confirmar con `GET` que el usuario ya no existe.
- [ ] **Step 4: Toda la suite del asistente** (`uv run pytest -q`), el build del front y los tests del backend sin base → PASS.
- [ ] **Step 5: Informar a Alejandro.**
  - Qué se verificó y qué no (por ejemplo, el deploy en Railway lo hace Facu).
  - Los dos PR que hay que abrir (backend `feat/asistente-voz` → `dev`; front, apilado sobre el #7). No se abren sin su OK.
