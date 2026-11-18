# Programación Multiplataforma 2026 - Pergamino

Proyecto desarrollado para la materia **Programación Multiplataforma 2026**: backend REST de una plataforma e-learning (Spring Boot + JPA + PostgreSQL).

## Integrantes

- Buccolini
- Mendez
- Rothamel

## Requisitos

- Java 17 o superior
- Una base PostgreSQL: la compartida del grupo en Aiven o una local

El proyecto Maven está en `backend/`. No hace falta instalar Maven: se usa `./mvnw`.

## Configuración

Copiá `.env.example` como `.env` en la **raíz** del repositorio y completá los datos de la base:

```env
DB_URL=jdbc:postgresql://localhost:5432/mydb
DB_USERNAME=user
DB_PASSWORD=pass
```

Para la base del grupo en Aiven, pedí los datos al grupo y agregá `?sslmode=require` al final de `DB_URL`. El `.env` no se sube al repositorio.

`JWT_SECRET` (opcional) es la clave con la que se firman los tokens de login; si no está, se usa una de desarrollo.
`JWT_EXPIRATION` (opcional) es la duración de los tokens en segundos; por defecto `864000` (10 días).

Las tablas se crean y actualizan solas al levantar la app (`spring.jpa.hibernate.ddl-auto=update`).

`CORS_ALLOWED_ORIGINS` (opcional) limita los orígenes del frontend que pueden llamar a la API;
es una lista separada por comas (por ejemplo `http://localhost:5173,https://mi-frontend.com`).
Si no está, se permite cualquier origen (`*`), pensado para desarrollo.

## Ejecución

Desde la raíz del repositorio (en Git Bash):

```bash
set -a && . ./.env && set +a
cd backend
./mvnw spring-boot:run
```

La aplicación se inicia en [http://localhost:8080](http://localhost:8080). Los endpoints están bajo `/api` (por ejemplo `/api/courses`).

## Docker

La imagen se construye desde la **raíz** del repositorio (el `Dockerfile` es multi-stage: la
etapa de build usa Maven + JDK y la imagen final solo lleva el JRE y el jar).

```bash
docker build -t pm2026-backend .
docker run --rm -p 8080:8080 --env-file .env pm2026-backend
```

Queda disponible en [http://localhost:8080](http://localhost:8080). `--env-file .env` le pasa
las mismas variables que usa la app en local; el `.env` **no** se copia a la imagen (está en
`.dockerignore`).

La memoria de la JVM se ajusta sola al límite del contenedor, así que para acotar la RAM
alcanza con `-m`:

```bash
docker run --rm -p 8080:8080 --env-file .env -m 512m pm2026-backend
```

Para cambiar parámetros puntuales de la JVM se usa `JAVA_OPTS`:

```bash
docker run --rm -p 8080:8080 --env-file .env -e JAVA_OPTS="-Xmx256m" pm2026-backend
```

Si en lugar de la base de Aiven usás una PostgreSQL en el host y `DB_URL` apunta a
`localhost`, en Linux hay que compartir la red del host:

```bash
docker run --rm --network host --env-file .env pm2026-backend
```

## Tests

```bash
cd backend
./mvnw test
```

`Pm2026ApplicationTests` levanta la app completa y necesita las variables del `.env` cargadas y una base accesible.

## Observabilidad

Con la app levantada:

| Endpoint | Acceso | Qué muestra |
|---|---|---|
| `GET /actuator/health` | público | `UP`/`DOWN` de la app, la base de datos y el disco |
| `GET /actuator/info` | admin | nombre, versión y Java |
| `GET /actuator/metrics` | admin | lista de métricas; `/actuator/metrics/{nombre}` muestra una |
| `GET /actuator/prometheus` | admin | todas las métricas en formato Prometheus |

Los endpoints de admin piden el header `Authorization: Bearer ...` con el token de `POST /administrator/auth`. Sin token responden 401, y con el token de otro rol responden 403.

```bash
TOKEN=$(curl -s -H 'Content-Type: application/json' \
  -d '{"email":"<admin>","password":"<clave>"}' \
  http://localhost:8080/administrator/auth | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s -H "Authorization: $TOKEN" http://localhost:8080/actuator/prometheus | grep pm2026_
```

Métricas propias:

| Métrica | Qué cuenta |
|---|---|
| `pm2026_auth_logins_total{role, result}` | logins por rol (`STUDENT`, `INSTRUCTOR`, `ADMINISTRATOR`) y resultado (`success`, `failure`) |
| `pm2026_enrollments_total` | inscripciones creadas |
| `pm2026_course_approvals_total{result}` | cursos aprobados (`approved`) y rechazados (`rejected`) |
| `http_server_requests_seconds_*{uri, method, status}` | cantidad y tiempo de respuesta por endpoint (lo da Spring) |
| `hikaricp_connections_*` | uso del pool de conexiones a la base, que tiene un máximo de 3 |

**Logs:** cada request recibe un `correlationId`. Si el cliente manda el header `X-Correlation-Id`, se usa ese; si no, se genera uno. Se devuelve en la respuesta y aparece entre corchetes en cada línea de log de esa request:

```
INFO ... [demo-facu-1] a.e.u.p.s.o.CorrelationIdFilter : GET /api/courses -> 200 (502 ms)
```

Para logs en JSON, agregá `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` al entorno.

## Probar la API

El login único es `POST /api/auth/token` con `{"email", "password"}` y sirve para los tres roles. Devuelve
`accessToken` (el JWT sin prefijo), `tokenType` (`Bearer`), `expiresIn` (segundos) y `role` (`STUDENT`,
`INSTRUCTOR` o `ADMINISTRATOR`). En Swagger UI se pega `Bearer <accessToken>` en **Authorize**. Los logins de
cada rol del TP6 (`/students/auth`, `/auth`, `/admin/auth`) siguen igual; los viejos (`/student/auth`,
`/instructor/auth`, `/administrator/auth`) siguen andando pero quedan deprecados.

En `Docs/Postman` hay una colección con todas las reglas de negocio. Importala en Postman y correla con el Collection Runner. Cada corrida crea sus propios datos de prueba.

## Documentación

- `Docs/UML`: diagrama de clases y diagrama de secuencia del login único (`login.md`)
- `Docs/SQL`: modelo relacional (DER) y script del esquema
- `AGENTS.md`: estructura del proyecto, convenciones y estado actual

### API REST

Con la aplicación en ejecución, Swagger UI está disponible en
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) y el contrato OpenAPI en
[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs). Los endpoints protegidos usan el esquema
`token` del botón **Authorize**; se ingresa el valor completo `Bearer <jwt>`.

Las respuestas de error usan `application/problem+json` y el esquema `ProblemDetail` (RFC 7807), con los campos
estándar `type`, `title`, `status`, `detail` e `instance`, más `code` y `timestamp`. Los errores de validación pueden
incluir `errors`, una lista de objetos con `field` y `message`.

La tabla muestra los códigos de cada operación. Todas también pueden responder `405` (método no permitido), `406`
(formato de respuesta no aceptable) y `500` (error inesperado). `Location` apunta al recurso recién creado.

| Endpoint | Códigos posibles |
| --- | --- |
| `GET /api/students` (opcional `courseId`) | `200, 400, 401, 403, 404` |
| `GET /api/students/{id}` | `200, 400, 401, 403, 404` |
| `POST /api/students` | `201 + Location, 400, 409, 415` |
| `PUT /api/students/{id}` | `200, 400, 401, 403, 404, 409, 415` |
| `DELETE /api/students/{id}` | `204, 400, 401, 403, 404, 409` |
| `GET /api/instructors` | `200` |
| `GET /api/instructors/{id}`, `GET /api/instructors/{id}/specialties` | `200, 400, 404` |
| `GET /api/instructors/{id}/revenue` | `200, 400, 401, 403, 404` |
| `POST /api/instructors` | `201 + Location, 400, 401, 403, 409, 415` |
| `PUT /api/instructors/{id}` | `200, 400, 401, 403, 404, 409, 415` |
| `DELETE /api/instructors/{id}` | `204, 400, 401, 403, 404, 409` |
| `PUT`/`DELETE /api/instructors/{id}/specialties/{specialtyId}` | `204, 400, 401, 403, 404, 409` |
| `GET /api/administrators` | `200, 401, 403` |
| `GET /api/administrators/{id}` | `200, 400, 401, 403, 404` |
| `POST /api/administrators` | `201 + Location, 400, 401, 403, 409, 415` |
| `PUT /api/administrators/{id}` | `200, 400, 401, 403, 404, 409, 415` |
| `DELETE /api/administrators/{id}` | `204, 400, 401, 403, 404, 409` |
| `GET /api/courses` (con `studentId`) | `200, 400, 401, 403, 404` |
| `GET /api/courses/{id}` | `200, 400, 404` |
| `POST /api/courses` | `201 + Location, 400, 401, 403, 404, 409, 415` |
| `PUT /api/courses/{id}` | `200, 400, 401, 403, 404, 409, 415` |
| `DELETE /api/courses/{id}` | `204, 400, 401, 403, 404, 409` |
| `GET /api/specialties` | `200` |
| `GET /api/specialties/{id}` | `200, 400, 404` |
| `POST /api/specialties` | `201 + Location, 400, 401, 403, 409, 415` |
| `PUT /api/specialties/{id}` | `200, 400, 401, 403, 404, 409, 415` |
| `DELETE /api/specialties/{id}` | `204, 400, 401, 403, 404, 409` |
| `GET /api/enrollments`, `GET /api/enrollments/revenue` | `200, 401, 403` |
| `GET /api/enrollments/{id}` | `200, 400, 401, 403, 404` |
| `POST /api/enrollments` | `201 + Location, 400, 401, 403, 404, 409, 415` |
| `GET /api/approval-requests` | `200, 400, 401, 403` |
| `GET /api/approval-requests/{id}` | `200, 400, 401, 403, 404` |
| `POST /api/approval-requests` | `201 + Location, 400, 401, 403, 404, 409, 415` |
| `POST /api/approval-requests/{id}/approve`, `POST /api/approval-requests/{id}/reject` | `200, 400, 401, 403, 404, 409, 415` |
| `POST /student/auth`, `POST /instructor/auth`, `POST /administrator/auth` | `200, 400, 401, 415` |
| `POST /instructor` (registro del TP6, público) | `201 + Location, 400, 409, 415` |
| `GET /instructor`, `GET /instructor/courses` | `200, 401, 403` |

`400` corresponde a entradas inválidas; `401` a falta o invalidez de autenticación; `403` a permisos insuficientes;
`404` a recursos inexistentes; `409` a conflictos de dominio o integridad; y `415` a un tipo de contenido no
soportado. `400` se documenta donde hay cuerpo, parámetros de ruta o de consulta; `415`, donde hay cuerpo. En
`GET /api/students`, el `404` aplica si se envía un `courseId` inexistente. En `GET /api/courses`, `401`, `403` y
`404` aplican cuando se envía `studentId`; el listado sin ese filtro es público.

### Rutas del TP6

El TP6 agrupa las rutas por rol. Conviven con las de `/api/...`, que sigue usando el front. Cada grupo acepta solo
el token de su rol y con el de otro responde `403`: un administrador no puede hacer acciones de instructor ni de
alumno (#38). Los nombres siguen el enunciado (`name`, `lastName`). Como en la tabla anterior, todas pueden
responder además `405`, `406` y `500`.

#### Administrador (#39)

| Endpoint | Qué hace | Códigos posibles |
| --- | --- | --- |
| `POST /admin/auth` | login: `{"token": "Bearer ..."}` | `200, 400, 401, 415` |
| `POST /admin/accounts` | alta (`email`, `password`, `name`, `lastName`) | `201 + Location, 400, 401, 403, 409, 415` |
| `GET /admin/accounts` | administradores: `id`, `name`, `lastName`, `email` | `200, 401, 403` |
| `DELETE /admin/accounts/{id}` | baja; `409` si es el mismo administrador del token | `204, 400, 401, 403, 404, 409` |
| `GET /admin/approval-requests` | solicitudes pendientes (`SUBMITTED`), de la más vieja a la más nueva | `200, 401, 403` |
| `PUT /admin/approval-requests/{id}` | resuelve con `status` (`APPROVED`/`REJECTED`) y `adminComment` | `200, 400, 401, 403, 404, 409, 415` |
| `GET /admin/courses` | cursos publicados, por fecha de inicio descendente | `200, 401, 403` |
| `GET /admin/courses/{id}` | detalle con instructor, `enrolledStudents` y `availableSeats` | `200, 400, 401, 403, 404` |
| `GET /admin/courses/{id}/students` | inscriptos con `enrollmentDate` y `finalAmount` | `200, 400, 401, 403, 404` |
| `GET /admin/instructors` | instructores con sus especialidades | `200, 401, 403` |
| `GET /admin/instructors/{id}` | datos, especialidades y `courseSummary` (cursos por estado) | `200, 400, 401, 403, 404` |
| `GET /admin/instructors/{id}/courses` | sus cursos publicados | `200, 400, 401, 403, 404` |
| `GET /admin/instructors/{id}/courses/pending` | sus cursos pendientes de aprobación | `200, 400, 401, 403, 404` |
| `GET /admin/students` | alumnos registrados | `200, 401, 403` |
| `GET /admin/students/{id}` | datos, `enrolledCourses` y `totalPaid` | `200, 400, 401, 403, 404` |
| `GET /admin/students/{id}/courses` | cursos en los que se inscribió | `200, 400, 401, 403, 404` |

#### Reportes del administrador (#40)

Los tres piden `fromDate` y `toDate` (`YYYY-MM-DD`, los dos días incluidos) y filtran por la fecha de inscripción.
Responden `400` si falta una fecha (`request.missing-parameter`), si no es una fecha válida
(`request.invalid-parameter`) o si `fromDate` es posterior a `toDate` (`request.invalid-date-range`).

| Endpoint | Qué devuelve | Códigos posibles |
| --- | --- | --- |
| `GET /admin/reports/revenue` | `fromDate`, `toDate`, `enrollments` y `total` pagado en el rango | `200, 400, 401, 403` |
| `GET /admin/reports/courses-revenue` | por curso con inscripciones en el rango: `enrollments` y `total`, del que más facturó al que menos | `200, 400, 401, 403` |
| `GET /admin/reports/enrollments` | cada inscripción con `basePrice` (el actual del curso), `finalAmount` y `discount` | `200, 400, 401, 403` |

#### Instructor (#41)

Salvo el registro y el login, piden token de instructor y solo operan sobre sus cursos: uno ajeno responde `403`, y
editar o borrar uno que ya salió de borrador responde `409`. Un administrador recibe `403`.

| Endpoint | Qué hace | Códigos posibles |
| --- | --- | --- |
| `POST /instructor` | registro sin token (`email`, `password`; `name` y `lastName` opcionales hasta que se resuelva el #13) | `201 + Location, 400, 409, 415` |
| `POST /auth` | login: `{"token": "Bearer ..."}` | `200, 400, 401, 415` |
| `GET /instructor/courses` | sus cursos: `id`, `name`, `description` | `200, 401, 403` |
| `GET /instructor/courses/{id}` | detalle con estado, cupo, inscriptos y precio base | `200, 400, 401, 403, 404` |
| `POST /instructor/courses` | alta en DRAFT (`title`, `description`, `startDate`, `endDate`, `maxCapacity`, `basePrice`, `specialty`) | `201 + Location, 400, 401, 403, 404, 409, 415` |
| `PUT /instructor/courses/{id}` | edita solo los campos que vienen | `200, 400, 401, 403, 404, 409, 415` |
| `DELETE /instructor/courses/{id}` | baja de un curso en DRAFT | `204, 400, 401, 403, 404, 409` |
| `POST /instructor/courses/{id}/approval-requests` | pide la aprobación (`instructorComment` opcional) | `201 + Location, 400, 401, 403, 404, 409, 415` |
| `GET /instructor/approval-requests` | sus solicitudes | `200, 401, 403` |
| `GET /instructor/courses/{id}/students` | inscriptos de un curso suyo | `200, 400, 401, 403, 404` |
| `GET /instructors/me/revenue` | lo acumulado: 65% del precio base por inscripción | `200, 401, 403` |
| `GET /instructor/revenue/enrollments` | total y detalle por inscripción; `fromDate` y `toDate` opcionales | `200, 400, 401, 403` |

#### Alumno (#42)

Salvo el registro y el login, piden token de alumno. El precio con descuento (`applicablePrice`) sale de la misma
regla que la inscripción (25% menos si ya se inscribió a otro curso de la misma especialidad en el mes), así el
precio que se muestra es el que se cobra.

| Endpoint | Qué hace | Códigos posibles |
| --- | --- | --- |
| `POST /students` | registro sin token (`name`, `lastName`, `dni`, `email`, `password`) | `201 + Location, 400, 409, 415` |
| `POST /students/auth` | login: `{"token": "Bearer ..."}` | `200, 400, 401, 415` |
| `GET /student/courses/available` | publicados que no empezaron, con cupo y sin los que ya cursa, con `basePrice`, `applicablePrice` y `loyaltyDiscount` | `200, 401, 403` |
| `GET /student/courses/{id}` | detalle con instructor, `availableSeats`, precios y `enrolled`; `404` si no está publicado | `200, 400, 401, 403, 404` |
| `POST /student/enrollments` | inscripción (`courseId`): `basePrice`, `finalAmount` y `discount` | `201 + Location, 400, 401, 403, 404, 409, 415` |
| `GET /students/enrollments` | sus inscripciones de la más nueva a la más vieja, con el curso y lo pagado | `200, 401, 403` |
