# AGENTS.md

Guía para agentes de IA que trabajan en este repositorio.

## Proyecto

Aplicación web para la materia **Programación Multiplataforma 2026** (grupo Pergamino).
Backend en **Spring Boot** con **JPA/Hibernate** y **PostgreSQL**.

- Remoto: `https://github.com/myrus1/pm2026-pergamino-grupo-x.git`
- Rama principal de desarrollo: `develop` (`main` queda estable; ver "Flujo git")
- Idioma del proyecto: **español** (comentarios, nombres de dominio y mensajes de commit en español).

## Estructura del repositorio

Arquitectura **por features** (*screaming architecture*): cada feature agrupa sus modelos,
servicios, repositorios, controladores y DTOs. No hay paquetes por capa (`entities/`,
`services/`, ...) sino por dominio.

```
.
├── AGENTS.md / README.md / .env.example   # raíz del repo (no es un proyecto Maven)
├── Dockerfile / .dockerignore             # imagen del backend (contexto de build: la raíz)
├── demo/                                  # <- maven root (acá está pom.xml y ./mvnw)
│   ├── src/main/java/com/example/demo/
│   │   ├── DemoApplication.java
│   │   ├── user/                 # feature: User, Student, Instructor, Administrator
│   │   │   ├── model/
│   │   │   ├── services/         # UserService (base) y StudentService, InstructorService, AdministratorService
│   │   │   ├── repositories/
│   │   │   ├── controllers/      # StudentController, InstructorController, AdministratorController, InstructorResource (TP4), StudentResource y AdministratorResource (TP5)
│   │   │   └── dto/              # StudentRequest/Response, Instructor..., Administrator..., CreateInstructorRequestDTO, AuthenticationRequestDTO
│   │   ├── course/               # feature: Course, CourseStatus, Specialty
│   │   │   ├── model/
│   │   │   ├── services/         # CourseService, SpecialtyService
│   │   │   ├── repositories/
│   │   │   ├── controllers/      # CourseController, SpecialtyController
│   │   │   └── dto/              # CourseRequest/Response, SpecialtyRequest/Response
│   │   ├── enrollment/           # feature: Enrollment, CourseEnrollmentException
│   │   │   ├── model/
│   │   │   ├── services/         # EnrollmentService
│   │   │   ├── repositories/
│   │   │   ├── controllers/      # EnrollmentController
│   │   │   └── dto/              # EnrollmentRequest/Response
│   │   ├── approval/             # feature: ApprovalRequest, ApprovalStatus, ApprovalRequestException
│   │   │   ├── model/
│   │   │   ├── services/         # ApprovalRequestService
│   │   │   ├── repositories/
│   │   │   ├── controllers/      # ApprovalRequestController
│   │   │   └── dto/              # ApprovalSubmitRequest, ApprovalResolveRequest, ApprovalRequestResponse
│   │   ├── auth/                 # feature TP5: AuthenticationException, AuthorizationException
│   │   │   ├── model/
│   │   │   └── services/         # AuthenticationService(Imp), AuthorizationService(Imp)
│   │   └── common/               # transversal: ApiExceptionHandler, Validaciones, ErrorResponse, PasswordEncoder, JwtTokenUtil, Rol, UsuarioAutenticado
│   ├── src/main/resources/application.properties
│   └── src/test/java/com/example/demo/   # tests por feature (dominio, servicios, controladores) + testutil/
└── Docs/                     # UML/ (diagrama de clases), SQL/ (DER y esquema), Postman/ (coleccion)
```

Importante: **el directorio Maven es `demo/`, no la raíz**. No existe `mvnw` en la raíz.

Al agregar una feature nueva: crear `<feature>/model`, `<feature>/services`,
`<feature>/repositories`, `<feature>/controllers` y `<feature>/dto` bajo
`com.example.demo`. Lo transversal a todas las features (manejo de errores,
validación de requests) vive en `common/`.

## Comandos

Siempre correr Maven desde `demo/` (desde la raíz, invocando `demo/mvnw -f demo/pom.xml`):

```bash
# Correr la app (el .env vive en la RAÍZ del repo)
set -a && . ./.env && set +a
cd demo && ./mvnw spring-boot:run

# Compilar / testear
cd demo && ./mvnw -q compile
cd demo && ./mvnw test

# Imagen Docker (se construye desde la RAÍZ del repo, no desde demo/)
docker build -t pm2026-backend .
docker run --rm -p 8080:8080 --env-file .env pm2026-backend
```

La app levanta en `http://localhost:8080`.

Los tests (`@SpringBootTest`) requieren que existan las variables de entorno y una
PostgreSQL accesible, si no falla al resolver `${DB_URL}`.

## Configuración

- Las credenciales de BD **no van en el código ni en el repo**: se leen de variables de
  entorno definidas en `application.properties` como `${DB_URL}`, `${DB_USERNAME}`,
  `${DB_PASSWORD}`. Plantilla en `.env.example`; `.env` está en `.gitignore`.
- `jwt.secret=${JWT_SECRET:...}`: clave HMAC512 de los tokens. Si no hay `JWT_SECRET` en el
  `.env` se usa un valor por defecto de desarrollo.
- CORS (`common/CorsConfig`): `cors.allowed-origins=${CORS_ALLOWED_ORIGINS:*}` acepta una lista
  de orígenes separada por comas (`http://localhost:5173,https://mi-frontend.com`). Por defecto
  `*` (desarrollo). Se permite el header `Authorization` y los métodos GET/POST/PUT/PATCH/DELETE/OPTIONS.
- `spring.jpa.hibernate.ddl-auto=update` → el esquema se actualiza solo; **no hay
  migraciones (Flyway/Liquibase)**. Cambios de esquema se generan desde las entidades.
- `spring.jpa.open-in-view=false` → la sesión JPA se cierra al salir del servicio. Todo lo
  que recorra o modifique relaciones LAZY (inscribir, crear/editar cursos, asignar
  especialidades) se hace dentro de un método `@Transactional` del servicio, nunca en el
  controlador.
- Stack: Spring Boot parent **4.1.1**, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-webmvc`, Lombok, PostgreSQL driver, Password4j (BCrypt en
  `common/PasswordEncoder`), DevTools, springdoc-openapi **3.2.0** (Swagger UI/OpenAPI).
  `java.version` en `pom.xml` = **17**.

## Convenciones de dominio (seguir el patrón de `<feature>/model`)

- **Paquetes por feature, no por capa**: `com.example.demo.<feature>.model` /
  `.services` / `.repositories`. Cada feature tiene sus tres subpaquetes.
- JPA: `@Entity` + `@Table(name = "plural_snake")` (`students`, `courses`), `@Id`
  `@GeneratedValue(strategy = IDENTITY)`, **todas las entidades tienen constructor sin
  argumentos** (lo exige JPA) y constructores de conveniencia explícitos.
- Herencia: `User` es `abstract` y base de `Student`, `Instructor`, `Administrator`
  (herencia `JOINED`).
- Enums persistidos con `@Enumerated(EnumType.STRING)` (`CourseStatus`, `ApprovalStatus`).
- Lombok de forma selectiva (`@Getter`, `@Setter` a nivel de campo); los getters de
  colecciones devuelven `Collections.unmodifiableSet(...)`.
- **Las colecciones del lado inverso de una relación (`mappedBy`) llevan `@JsonIgnore`**
  para cortar ciclos al serializar a JSON (`Student.enrollments`, `Course.enrollments`,
  `Instructor.courses`, `Specialty.courses`, `Specialty.instructors`).
- **Visibilidad**: como las clases están repartidas en varios paquetes, todo lo que cruza
  entre features es `public` (`addX`, `removeX`, `setX` de relaciones, constructores de
  entidades). Solo se mantiene `package-private` lo que se usa dentro del mismo paquete
  (p. ej. `Student.applyCourse(course, fecha)` para testear sin reloj real, y
  `Specialty.addCourse/removeCourse`).
- La **lógica de negocio vive en las entidades** (p. ej. `Student.applyCourse(...)` valida
  estado, fecha y capacidad y lanza `CourseEnrollmentException`), no en services/controllers.
  Usar `Objects.requireNonNull` con mensajes para precondiciones.
- Nombres de atributos/relaciones en inglés; **comentarios y mensajes de error en español**.
  Relaciones `@ManyToOne(fetch = LAZY)` con `@JoinColumn(nullable = false)`; en
  `Instructor.specialties` también `LAZY`.
- **Servicios**: `@Service` + `@Transactional` en la clase, `@Transactional(readOnly = true)`
  en los métodos de lectura, constructor único con el repositorio (y los servicios de otras
  features que necesite) inyectado. CRUD básico:
  `getAll()`, `getById(id)`, `save(x)`, `delete(id)`. `getById` lanza
  `jakarta.persistence.EntityNotFoundException` si no existe.
- **Usuarios**: `StudentService`, `InstructorService` y `AdministratorService` heredan de
  `UserService<T extends User>`, que da `create(x)` y `update(x)` guardando la contraseña con
  BCrypt (`PasswordEncoder`). Los servicios de usuarios **no tienen `save`**: para no guardar
  nunca una contraseña sin hash, se usa siempre `create`/`update`.
  Una subclase puede sobreescribir `validarEmailDisponible` (lo hace `InstructorService`).

## Flujo git

- Ramas: `main` (estable) ← `develop` (integración) ← ramas feature
  (p. ej. `entities-feature`, `compose-db`, `dev`), mergeadas mediante **Pull Request**.
- Trabajar sobre `develop` creando una rama feature; no commitear directo en `main`.
- La rama `dev` contiene el refactor a arquitectura por features + servicios.
- Commits convencionales, descripción en español: `feat(config): ...`,
  `fix(entities): ...`, `refactor(domain): ...`.
- Ejecutar `./mvnw -q compile` (y `test` si hay tests) antes de commitear.

## Matriz de permisos (rama `permisos-roles`)

Toda la API pide el token en el header `Authorization` (sin prefijo `Bearer` adicional:
el valor guardado por los logins ya viene como `"Bearer ..."`). El controlador llama a
`AuthorizationService.autorizar(token, Rol...)`; si el rol no alcanza devuelve 403, y si el
token falta/vence 401 (`ApiExceptionHandler` los mapea a `{"mensaje": ...}`).
`UsuarioAutenticado` (en `common/`, no en `auth/` para no armar ciclos de paquetes) expone
`exigirPropio(idPropietario, mensaje)`: **el administrador siempre pasa**, los demás solo si
el id coincide.

| Endpoint | ALUMNO | INSTRUCTOR | ADMINISTRADOR | Público |
| --- | --- | --- | --- | --- |
| `POST /api/students` (registro) | ✅ | ✅ | ✅ | ✅ **sin token** |
| `GET /api/courses`, `GET /api/courses/{id}`, `GET /api/instructors`, `GET /api/instructors/{id}`, `GET /api/specialties` | ✅ | ✅ | ✅ | ✅ |
| `POST /api/instructors`, `PUT/DELETE /api/instructors/{id}`, `PUT/DELETE /api/instructors/{id}/specialties/{sid}` | ❌ | ❌ | ✅ | ❌ |
| `POST/PUT/DELETE /api/administrators` | ❌ | ❌ | ✅ | ❌ |
| `POST/PUT/DELETE /api/specialties` | ❌ | ❌ | ✅ | ❌ |
| `GET /api/students` (lista), `GET /api/students/{id}`, `PUT/DELETE /api/students/{id}` | ❌ | ❌ | ✅ | ❌ |
| `GET /api/students?courseId=` | ❌ | solo su curso | ✅ | ❌ |
| `GET /api/courses?studentId=` | solo el suyo | ❌ | ✅ | ❌ |
| `POST/PUT/DELETE /api/courses` | ❌ | solo los suyos, y solo en **DRAFT** | ✅ | ❌ |
| `POST /api/enrollments` | solo a sí mismo | ❌ | ✅ (inscribe a cualquiera) | ❌ |
| `GET /api/enrollments`, `GET /api/enrollments/revenue` | ❌ | ❌ | ✅ | ❌ |
| `GET /api/enrollments/{id}` | solo la suya | solo las de sus cursos | ✅ | ❌ |
| `GET /api/instructors/{id}/revenue` | ❌ | solo la suya | ✅ | ❌ |
| `POST /api/approval-requests` | ❌ | solo de sus cursos | ✅ | ❌ |
| `GET /api/approval-requests` (`?status=`/`?instructorId=`) | ❌ | solo las suyas (ignora el filtro) | ✅ | ❌ |
| `GET /api/approval-requests/{id}`, `.../{id}/approve`, `.../{id}/reject` | ❌ | solo lectura de las suyas | ✅ | ❌ |

- Logins: `POST /student/auth`, `POST /instructor/auth` y `POST /administrator/auth`
  (los tres reciben `AuthenticationRequestDTO`: `email` + `password` y devuelven
  `{"token": "Bearer ..."}`).
- El instructor **no** elige el dueño de un curso: `CourseService` usa el id del token y solo
  el administrador puede mandar `instructorId` en el body.
- Un curso que salió de DRAFT no se edita ni se borra: `CourseStateException` → 409.
- `administratorId` ya no se manda en `ApprovalResolveRequest`; sale del token.
- Pruebas: `testutil/AutorizacionPrueba` (`porDefecto`, `sinToken`, `sinPermiso`,
  `comoAlumno`, `comoInstructor`) y la carpeta `9 - Permisos (401, 403 y 409)` de la
  colección de `Docs/Postman/`.

## Documentación de la API (Swagger/OpenAPI)

La API se documenta con **springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui`
**3.2.0**, compatible con Spring Boot 4):

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (redirige a
  `/swagger-ui/index.html`). **Spec JSON**: `http://localhost:8080/v3/api-docs`. Las rutas
  se configuran en `application.properties` (`springdoc.swagger-ui.path`,
  `springdoc.api-docs.path`).
- **`common/OpenApiConfig.java`**: define la info de la API y el security scheme `token`
  (apiKey en el header `Authorization`), y registra un `OperationCustomizer`
  (`QuitarHeaderDeAutorizacion`) que saca el parámetro `Authorization` que springdoc infiere
  de cada `@RequestHeader`. El token no es un parámetro más del endpoint: se envía desde el
  botón **Authorize** de Swagger UI con el valor completo `Bearer <jwt>` (igual que lo
  devuelven los logins y como lo usa Postman: `Authorization: {{tokenAdmin}}`).
- **Qué pide token**: `@SecurityRequirement(name = "token")` en los endpoints protegidos de
  la matriz de permisos. Va a nivel de **clase** en los controladores 100% protegidos
  (`AdministratorController`, `EnrollmentController`, `ApprovalRequestController`) y a nivel
  de **método** en los que también tienen endpoints públicos (`StudentController`,
  `CourseController`, `InstructorController`, `SpecialtyController`, `InstructorResource`).
  springdoc fusiona lo de la clase con lo del método, y los públicos quedan sin anotación.
- **Pruebas**: `common/OpenApiConfigTest` (unitario: info, esquema y limpieza del header) y
  `DocumentacionApiTest` (levanta el contexto completo, lee `/v3/api-docs` y verifica que
  los públicos no tengan `security`, que los protegidos sí tengan `token` y que ninguna
  operación declare el header `Authorization` como parámetro).
- **En producción** `/v3/api-docs` y `/swagger-ui.html` quedan públicos: apagarlos con
  `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false`.
- **Tests y BD**: los tests que levantan el contexto completo usan `@ActiveProfiles("test")`,
  que aplica `src/test/resources/application-test.properties` (pool de 2 conexiones). La BD
  compartida del curso acepta **20 conexiones** en total y cada contexto de test usa Hikari
  por defecto (10): con dos contextos en la misma JVM se pasaba del límite y fallaba
  `Unable to determine Dialect without JDBC metadata`. **No** crear un `application.properties`
  en `src/test/resources`: sombrea el de `src/main/resources` y se pierden `DB_URL`,
  `jwt.secret`, etc.

## Pendiente / state actual

- Modelos, repositorios y servicios implementados por feature.
- Docker: `Dockerfile` multi-stage en la raíz (build con Maven + JDK 17, runtime JRE 17
  alpine con usuario no-root) y `.dockerignore` que excluye `.env`. Las variables se pasan
  en runtime con `--env-file .env`; no se hornean en la imagen. El heap sigue el límite de
  memoria del contenedor (`-XX:MaxRAMPercentage=60`) y se puede ajustar con `JAVA_OPTS`.
- API REST implementada: CRUD bajo `/api/students`, `/api/instructors`,
  `/api/administrators`, `/api/courses`, `/api/specialties`, `/api/enrollments` y
  `/api/approval-requests`.
  Los controladores y DTOs viven en `<feature>/controllers` y `<feature>/dto`;
  `ApiExceptionHandler`, `Validaciones` y `ErrorResponse` son transversales
  (paquete `common/`).
  `POST /api/enrollments` valida las reglas de negocio via
  `EnrollmentService.enroll` → `Student.applyCourse` (errores 409). `ApiExceptionHandler` mapea excepciones a
  400/404/409 con cuerpo `{"mensaje": ...}`.
- Flujo de aprobación: `POST /api/approval-requests` (curso en DRAFT → PENDING_APPROVAL),
  `POST /api/approval-requests/{id}/approve` (→ PUBLISHED) y `/{id}/reject` (→ DRAFT);
  `GET /api/approval-requests?status=&instructorId=` para listar. Las reglas viven en
  `ApprovalRequest` y responden 409. El estado del curso no se recibe en `CourseRequest`:
  solo cambia por este flujo.
- TP4: `POST /instructor` (`user/controllers/InstructorResource`) recibe
  `CreateInstructorRequestDTO` (record con email, password, firstName y lastName), delega en
  `InstructorService.create` (email unico + BCrypt) y responde con `ResponseEntity` 201, o 409
  si la creacion falla. Convive con el CRUD de `/api/instructors`.
- TP5: `POST /instructor/auth`, `POST /student/auth` y `POST /administrator/auth` reciben
  `AuthenticationRequestDTO` (email, password) y devuelven `{"token": "Bearer ..."}`
  (JWT HMAC512, vence en 10 dias, subject = email, con claims `rol` y `userId`), o 401.
  `GET /instructor` y `GET /instructor/courses` leen el header `Authorization`, validan el token
  con `AuthorizationService` y devuelven solo los cursos de ese instructor
  (`InstructorCoursesResponseDTO`: id, name, description), o 403. Los servicios de `auth/` lanzan
  `AuthenticationException`/`AuthorizationException` (unchecked), que son las que
  `ApiExceptionHandler` convierte en 401/403.
- Recaudación: `GET /api/instructors/{id}/revenue` (65% del precio base por inscripción,
  sin importar descuentos; regla en `Enrollment.instructorRevenue`) y
  `GET /api/enrollments/revenue` (total pagado por los alumnos).
- Listados: `GET /api/students?courseId=` (alumnos inscriptos en un curso) y
  `GET /api/courses?studentId=` (cursos del alumno, por fecha de inicio descendente).
- Las inscripciones no tienen `DELETE` (no se pueden cancelar). Un curso solo se crea o
  edita con una especialidad de su instructor (`Instructor.validateSpecialty`).
  `DataIntegrityViolationException` (DNI/email duplicado, borrar algo en uso) responde 409.
- Tests unitarios del dominio (`CourseTest`, `StudentTest`, `InstructorTest`,
  `SpecialtyTest`, `EnrollmentTest`), de los servicios (contrato de `getById`, ahora con el
  dueño) y de los controladores (`*ControllerTest`, slice `@WebMvcTest`, con
  `testutil/AutorizacionPrueba`), todos sin BD salvo `contextLoads`.
  Todavia no hay tests de persistencia (JPA). Suite: **345 tests**.
- **Pendiente conocido: el primer administrador.** `/api/administrators` exige token de admin,
  asi que no hay como crear el primer admin desde la API (ciclo). Mientras se decide
  (seed desde `ADMIN_EMAIL`/`ADMIN_PASSWORD`, INSERT SQL o alta publica condicional), la
  coleccion `Docs/Postman/` no puede correr de punta a punta: la carpeta
  `0 - Autenticacion (JWT)` usa las variables `adminEmail`/`adminPassword` y responde 401
  hasta que exista ese admin.
