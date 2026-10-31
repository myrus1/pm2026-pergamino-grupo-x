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
├── demo/                                  # <- maven root (acá está pom.xml y ./mvnw)
│   ├── src/main/java/com/example/demo/
│   │   ├── DemoApplication.java
│   │   ├── user/                 # feature: User, Student, Instructor, Administrator
│   │   │   ├── model/
│   │   │   ├── services/         # StudentService, InstructorService, AdministratorService
│   │   │   ├── repositories/
│   │   │   ├── controllers/      # StudentController, InstructorController, AdministratorController
│   │   │   └── dto/              # StudentRequest/Response, Instructor..., Administrator...
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
│   │   ├── approval/             # feature: ApprovalRequest, ApprovalStatus (solo modelo)
│   │   │   └── model/
│   │   └── common/               # transversal: ApiExceptionHandler, Validaciones, ErrorResponse
│   ├── src/main/resources/application.properties
│   ├── src/main/Docs/UML/Diagrama UML.pdf   # diagrama de clases (modelo de dominio)
│   └── src/test/java/com/example/demo/DemoApplicationTests.java  # solo contextLoads
└── .github/modernize/      # hooks de una extensión externa; no modificar ni usar
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
```

La app levanta en `http://localhost:8080`.

Los tests (`@SpringBootTest`) requieren que existan las variables de entorno y una
PostgreSQL accesible, si no falla al resolver `${DB_URL}`.

## Configuración

- Las credenciales de BD **no van en el código ni en el repo**: se leen de variables de
  entorno definidas en `application.properties` como `${DB_URL}`, `${DB_USERNAME}`,
  `${DB_PASSWORD}`. Plantilla en `.env.example`; `.env` está en `.gitignore`.
- `spring.jpa.hibernate.ddl-auto=update` → el esquema se actualiza solo; **no hay
  migraciones (Flyway/Liquibase)**. Cambios de esquema se generan desde las entidades.
- `spring.jpa.open-in-view=false` → la sesión JPA se cierra al salir del servicio. Todo lo
  que recorra o modifique relaciones LAZY (inscribir, crear/editar cursos, asignar
  especialidades) se hace dentro de un método `@Transactional` del servicio, nunca en el
  controlador.
- Stack: Spring Boot parent **4.1.1**, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-data-jdbc`, `spring-boot-starter-webmvc`, Lombok, PostgreSQL driver,
  DevTools. `java.version` en `pom.xml` = **17** (el README dice Java 21+; el POM manda
  para compilar).

## Convenciones de dominio (seguir el patrón de `<feature>/model`)

- **Paquetes por feature, no por capa**: `com.example.demo.<feature>.model` /
  `.services` / `.repositories`. Cada feature tiene sus tres subpaquetes (hoy `approval`
  solo tiene `model`).
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

## Flujo git

- Ramas: `main` (estable) ← `develop` (integración) ← ramas feature
  (p. ej. `entities-feature`, `compose-db`, `dev`), mergeadas mediante **Pull Request**.
- Trabajar sobre `develop` creando una rama feature; no commitear directo en `main`.
- La rama `dev` contiene el refactor a arquitectura por features + servicios.
- Commits convencionales, descripción en español: `feat(config): ...`,
  `fix(entities): ...`, `refactor(domain): ...`.
- Ejecutar `./mvnw -q compile` (y `test` si hay tests) antes de commitear.

## Pendiente / state actual

- Modelos, repositorios y servicios implementados por feature.
- API REST implementada: CRUD bajo `/api/students`, `/api/instructors`,
  `/api/administrators`, `/api/courses`, `/api/specialties` y `/api/enrollments`.
  Los controladores y DTOs viven en `<feature>/controllers` y `<feature>/dto`;
  `ApiExceptionHandler`, `Validaciones` y `ErrorResponse` son transversales
  (paquete `common/`).
  `POST /api/enrollments` valida las reglas de negocio via
  `EnrollmentService.enroll` → `Student.applyCourse` (errores 409). `ApiExceptionHandler` mapea excepciones a
  400/404/409 con cuerpo `{"mensaje": ...}`.
- `approval/` solo tiene `model`: `ApprovalRequest`/`ApprovalStatus` no tienen servicio ni
  repositorio todavía.
- Tests unitarios del dominio (`CourseTest`, `StudentTest`, `InstructorTest`,
  `SpecialtyTest`, `EnrollmentTest`), de los servicios (contrato de `getById`) y de los
  controladores (`*ControllerTest`, slice `@WebMvcTest`), todos sin BD salvo `contextLoads`.
  Todavia no hay tests de persistencia (JPA).
- El `README.md` tiene comandos desactualizados (`cd proyecto-pm-2026`, `./mvnw` desde la
  raíz); los comandos válidos son los de la sección "Comandos".
