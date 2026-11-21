# Asistente por voz con MCP para PM2026 (extra)

Reemplaza a `2026-11-19-mcp-design.md` (MCP en Java adentro del backend), que queda archivado.

## Para qué

Nelson, el profesor, propuso un extra: un MCP en Python que permita manejar la plataforma por voz, por ejemplo "agregá un usuario" o "listá los alumnos". Facu lo va a mostrar en clase desde Railway, con el backend que ya tiene ahí y la base de Aiven.

Sale bien si en la demo alguien aprieta un botón en el front, dice un pedido en castellano y la acción se hace en la plataforma con sus permisos: crear un alumno, listar cursos, aprobar una solicitud, ver un reporte. El resultado aparece en pantalla.

## Objetivos

- Un botón en el front de PM2026: se mantiene apretado mientras se habla y al soltarlo se manda el audio, sin vista previa. El grabador sale de G Comunicaciones (es del mismo autor), adaptado.
- Un servicio Python, "el asistente", que transcribe el audio, entiende el pedido y lo ejecuta llamando a herramientas de un servidor MCP en Python.
- El servidor MCP cubre lo que ofrece la API para los tres roles (alumno, instructor y administrador), no solo el registro. Ejecuta todo a través de la API REST del backend, con el token del usuario logueado.
- Todo con herramientas gratuitas: la capa gratuita de Groq (una clave) para la transcripción y para el modelo, más las librerías abiertas. Railway ya lo usa Facu.

## No objetivos

- Activar por palabra clave, con el micrófono escuchando siempre. Solo se activa con el botón.
- Respuesta hablada (texto a voz). La respuesta se muestra en pantalla.
- Cambios en el backend Java. El asistente usa la API tal cual está.
- Un nombre o personaje para el asistente.

## Anti-objetivos (fracaso aunque funcione)

- Que el asistente haga algo que el rol del usuario no puede hacer en la API. El token es siempre el del usuario logueado y nunca uno de servicio.
- Que borre datos sin que nadie lo confirme.
- Que invente datos que faltan: un DNI o un email que nadie dijo.
- Que tenga costos: nada pago, ninguna clave de G Comunicaciones.
- Que rompa el front o el backend actuales.

## Restricciones

- Backend: Spring Boot en Railway, contra Aiven, que es la base compartida.
  - El pool de conexiones es de 3 y las rutas no tienen paginación.
  - Errores en formato `ProblemDetail` con `code`.
  - El login único `POST /api/auth/token` devuelve el JWT. El claim `rol` vale ALUMNO, INSTRUCTOR o ADMINISTRADOR y el claim `userId` trae el id.
- Front: React con TypeScript y Vite, en `myrus1/front-pm2026-pergamino-grupo-x`. Solo tenemos lectura, así que los cambios van por PR desde el fork. El PR #7 (adaptar el front a la API, rama `feature/30-adaptar-front-api`) sigue abierto y este trabajo sale de esa rama.
- Python: el SDK oficial de MCP pide 3.10 o más nuevo y esta compu tiene 3.9.6. Se usa Python 3.12, instalado con `uv`.
- Groq gratis: tiene límites por minuto y por día. Si se pasan, responde 429 y no cobra. Los límites reales se ven en la consola de Groq.
- Mientras se desarrolla no se crean datos en Aiven. Los tests usan mocks. La primera prueba real que escribe en Aiven se hace con el OK explícito de Alejandro.

## Enfoques considerados

| Enfoque | Por qué sí o no |
|---|---|
| **Groq gratis: Whisper + modelo con herramientas (elegido)** | Una sola clave y gratis. Funciona desde Railway y la API es compatible con la de OpenAI. |
| Claude + Whisper de OpenAI | Pide dos claves y las dos cobran. Descartado. |
| Todo local (faster-whisper + Ollama) | No pide claves, pero no corre en Railway, necesita una compu potente y es lento. Descartado para la demo. |
| MCP en Java adentro del backend | Lo descartó el pedido del profesor: tiene que ser Python y por voz. |

## Diseño

### Piezas

```
Front (React)            Asistente (Python, Railway)                 Backend (Java, Railway)
botón 🎙 ── audio + JWT ──▶ POST /voz
                          1. Groq Whisper → texto
                          2. Groq LLM elige herramientas ──▶ MCP /mcp ── HTTP + JWT ──▶ API REST
                          3. respuesta  ◀────────────────────────────────────────────── JSON
◀── {transcripcion, respuesta, acciones, deshacer?, pendiente?}
```

- Carpeta `asistente/` en el repo del backend, al lado de `backend/`. Es un proyecto Python con `pyproject.toml` y `uv.lock`, y Railway lo despliega como un servicio aparte.
- Un solo proceso FastAPI con dos cosas montadas:
  - `POST /voz`: lo usa el front.
  - `/mcp`: el servidor MCP (FastMCP del SDK oficial `mcp`, transporte Streamable HTTP). También se puede conectar desde Claude Code con un `.mcp.json`, para mostrar que es un MCP estándar.
- El agente de `/voz` se conecta como cliente MCP a su propio `/mcp` y le reenvía el `Authorization` del usuario. Así todo pasa por el protocolo MCP, que es lo que pidió el profesor.

### Servidor MCP: herramientas

- **Implementación.** Cada herramienta es una función Python que llama a un endpoint de la API con `httpx` y el token recibido. Las validaciones y los permisos los hace el backend.
- **Dónde está el backend.** La URL sale de `BACKEND_URL`.
- **Nombres y descripciones.** Nombres en castellano snake_case. Cada descripción dice qué rol la puede usar y qué datos pide.
- **Cobertura por rol**, con los endpoints vigentes (las rutas del TP6 y las de `/api`, sin los logins viejos):
  - **Común:** `quien_soy` (lee los claims del token), `buscar_cursos`, `listar_especialidades`.
  - **Alumno:**
    - cursos disponibles con precio;
    - detalle de curso;
    - inscribirse;
    - mis inscripciones.
  - **Instructor:**
    - mis cursos;
    - crear, editar y borrar un curso en borrador;
    - alumnos de mi curso;
    - pedir aprobación;
    - mis solicitudes;
    - lo recaudado.
  - **Administrador:**
    - alta, listado y baja de alumnos, instructores y administradores;
    - asignar o quitar especialidades a un instructor;
    - especialidades (alta y baja);
    - solicitudes pendientes y aprobar o rechazar;
    - cursos, alumnos e instructores con detalle;
    - reportes de recaudación e inscripciones por fechas.
  - **Registro público:** `registrar_alumno` y `registrar_instructor` también funcionan sin token.
- **Etiquetas por herramienta:** roles que la pueden usar, `lectura`, `escritura` o `borrado`, y, si se puede deshacer, cómo (endpoint y método).
- **Fechas y listas.** Las fechas se aceptan como `YYYY-MM-DD`; el agente le pasa al modelo la fecha de hoy para que resuelva "este mes". Las listas devuelven a lo sumo 20 elementos y dicen cuántos más hay.
- **Contraseña en las altas.** Todas las altas exigen `password` y nadie la dicta. La herramienta genera una aleatoria de 10 caracteres y la devuelve en el resultado. El panel la muestra una sola vez, con un botón para copiarla, y no queda en logs ni en el historial. *(Decisión del diseño: dictarla en voz alta frente a la clase es inseguro y Whisper la transcribiría mal.)*
- **Qué se confirma.** Solo los borrados. Inscribirse, aprobar o rechazar una solicitud y el registro público se ejecutan directo aunque no tengan vuelta atrás: dictar el pedido ya es la intención, como pidió el grupo. *(Decisión del diseño.)*
- **Errores.** Un error de la API (400, 401, 403, 404, 409) se devuelve como resultado de herramienta con `code` y mensaje, para que el modelo lo explique ("no tenés permiso", "ese email ya existe"). No se tira una excepción.

### Agente (`POST /voz`)

1. **Entrada.** Recibe un `multipart` con `audio`, el header `Authorization` (opcional: sin token solo hay herramientas públicas) e `historial` opcional (los últimos turnos, en JSON). El historial permite responder "sí" a una confirmación o completar un dato que faltó.
2. **Transcripción.** Groq `whisper-large-v3-turbo`, con `language=es`.
3. **Herramientas visibles.** Lee el rol del JWT, sin validar la firma porque eso lo hace el backend, y solo le muestra al modelo las herramientas de ese rol. Son unas 15 o 20 por rol y no las 50 juntas, porque un modelo abierto se confunde con muchas.
4. **Modelo, configurable.** Se usa un cliente con formato OpenAI y tres variables: `LLM_BASE_URL`, `LLM_MODEL` y `LLM_API_KEY`.
   - **Por defecto, Groq** con un modelo que soporte tool calling. Se prueban `openai/gpt-oss-120b` y `llama-3.3-70b-versatile`, y queda el que mejor responda.
   - **Alternativas sin tocar código:** Gemini (endpoint compatible con OpenAI de Google AI Studio, capa gratuita) y GitHub Models. Se usan si la consola de esa cuenta muestra cupo suficiente para la demo.
   - **Transcripción:** siempre Groq Whisper (`GROQ_API_KEY`).
   - *(Decisión del diseño. Los cupos gratuitos de Gemini y GitHub Models pueden ser de 20 a 50 pedidos por día, y cada dictado gasta de 2 a 5.)*
   - El prompt del sistema dice: responder en castellano y breve; no inventar datos (si falta uno, pedirlo); antes de una herramienta de `borrado`, pedir confirmación.
   - Se permiten hasta 5 llamadas a herramientas por pedido.
5. **Confirmación de borrados.** El agente no ejecuta una herramienta de `borrado` en el mismo turno en que el modelo la pide. Devuelve `pendiente` con la descripción ("¿Borro al alumno Juan Pérez?") y el front muestra Sí/No. Si el usuario dice "sí" o hace clic en Sí, el próximo pedido trae `confirmar=<id>` y se ejecuta. Esto no depende de que el modelo obedezca: lo hace cumplir el código.
6. **Respuesta:** `{transcripcion, respuesta, acciones: [{herramienta, ok}], datos?, deshacer?, pendiente?}`.
   - `datos` es la tabla de un listado, para mostrarla.
   - `deshacer` es la llamada inversa de una creación, por ejemplo `DELETE /api/students/{id}`.
7. **Errores.** Un 429 de Groq se responde como "El asistente está saturado, probá en un minuto". Un audio de más de 60 segundos o de más de 10 MB se rechaza con 400.

### Front

- **Grabador.** Se trasladan `useAudioRecorder` y la parte de grabación de `AudioRecorderPanel` de G Comunicaciones, pasados a TypeScript. Se saca la vista previa y el reproductor: es mantener apretado, hablar y soltar.
- **Dónde está el botón.** Va en el `Layout` cuando hay sesión y también en la portada, donde sirve para el registro público.
- **Panel de respuesta.** Muestra lo transcripto, la respuesta y la tabla de `datos` si viene. También el botón "Deshacer" (10 segundos), que llama al endpoint de `deshacer` con el cliente de API del front, y Sí/No cuando hay un `pendiente`.
- **Configuración y estados.** La URL del asistente sale de `VITE_ASISTENTE_URL`. Si no está definida, el botón no aparece. El historial de la conversación vive en memoria y se borra al recargar.
- **Datos actualizados.** Después de una acción que escribe, la página actual vuelve a pedir sus datos para que se vea el cambio.

### Despliegue

- **Servicio en Railway.** Nuevo servicio `asistente` en el proyecto de Facu, que construye desde `asistente/`. Variables:
  - `GROQ_API_KEY`;
  - `LLM_BASE_URL`, `LLM_MODEL` y `LLM_API_KEY` (por defecto Groq);
  - `BACKEND_URL` (la URL interna o pública del backend);
  - `ALLOWED_ORIGINS` (el dominio del front).
- **Clave de Groq.** Es la de una cuenta del grupo, en la capa gratuita, y no se sube al repo.
- **"Deshacer" con rutas `/api`.** El front lo llama siempre con una ruta `/api/...` (por ejemplo `DELETE /api/students/{id}`), porque el nginx del front solo reenvía `/api/` al backend.
- **Ramas y deploy.**
  - El front va en un PR desde el fork, apilado sobre el #7 (`feature/30-adaptar-front-api`).
  - El backend no cambia, pero el de Railway tiene que estar al día con `dev` para tener las rutas del TP6.
  - Antes de la demo, Facu vuelve a desplegar el backend desde `dev` y el front desde la rama del PR (o desde `dev` del front, si ya se mergeó), con `VITE_ASISTENTE_URL` definida.
- **CORS del backend.** `CORS_ALLOWED_ORIGINS` ya existe. El "Deshacer" lo llama el front directo al backend, así que no hay que cambiar nada.

### Pruebas

- **Tests con pytest, sin red:**
  - las herramientas del MCP contra un backend falso (`respx`): el endpoint y el método correctos, el token reenviado y los errores de la API devueltos como resultado;
  - el agente con Groq simulado: que filtra herramientas por rol, que un `borrado` queda pendiente y solo se ejecuta con `confirmar`, el límite de 5 llamadas y el 429.
- **Prueba del MCP.** El MCP Inspector CLI contra el servicio local: `tools/list` y una lectura.
- **Prueba de punta a punta.** Primero lecturas por voz contra el backend local, y después escrituras con el OK explícito de Alejandro.
- **Front.** El build y el lint del front pasan. Se prueba en el navegador con el backend local.

## Qué queda para quien lo construya

- La lista exacta de herramientas, a partir de los controladores, sin duplicar rutas viejas y nuevas que hacen lo mismo: se usa la del TP6 si existe.
- El texto de las descripciones y del prompt del sistema.
- El modelo de Groq: se prueba con 5 pedidos de ejemplo y se queda el que mejor elige herramientas.
- El diseño visual del botón y del panel, respetando `src/index.css` del front.

## Riesgos abiertos

- **Límites del plan gratuito de Groq.** Si se pasan, se ve un mensaje claro y nada más.
- **Errores al dictar.** Los números y los emails dictados se transcriben mal. Mitigación: el panel muestra lo transcripto y lo que se hizo, y ofrece "Deshacer".
- **Rutas nuevas.** Si el front del PR #7 cambia antes de mergearse, hay que rebasar.
- **Tests de documentación.** El backend no cambia, así que `ApiDocumentationTest` no se ve afectado.
