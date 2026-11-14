# Programación Multiplataforma 2026 - Pergamino

Proyecto desarrollado para la materia **Programación Multiplataforma 2026**: backend REST de una plataforma e-learning (Spring Boot + JPA + PostgreSQL).

## Integrantes

- Buccolini
- Mendez
- Rothamel

## Requisitos

- Java 17 o superior
- Una base PostgreSQL: la compartida del grupo en Aiven o una local

El proyecto Maven está en `demo/`. No hace falta instalar Maven: se usa `./mvnw`.

## Configuración

Copiá `.env.example` como `.env` en la **raíz** del repositorio y completá los datos de la base:

```env
DB_URL=jdbc:postgresql://localhost:5432/mydb
DB_USERNAME=user
DB_PASSWORD=pass
```

Para la base del grupo en Aiven, pedí los datos al grupo y agregá `?sslmode=require` al final de `DB_URL`. El `.env` no se sube al repositorio.

`JWT_SECRET` (opcional) es la clave con la que se firman los tokens de login; si no está, se usa una de desarrollo.

Las tablas se crean y actualizan solas al levantar la app (`spring.jpa.hibernate.ddl-auto=update`).

`CORS_ALLOWED_ORIGINS` (opcional) limita los orígenes del frontend que pueden llamar a la API;
es una lista separada por comas (por ejemplo `http://localhost:5173,https://mi-frontend.com`).
Si no está, se permite cualquier origen (`*`), pensado para desarrollo.

## Ejecución

Desde la raíz del repositorio (en Git Bash):

```bash
set -a && . ./.env && set +a
cd demo
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
cd demo
./mvnw test
```

`DemoApplicationTests` levanta la app completa y necesita las variables del `.env` cargadas y una base accesible.

## Probar la API

En `Docs/Postman` hay una colección con todas las reglas de negocio. Importala en Postman y correla con el Collection Runner. Cada corrida crea sus propios datos de prueba.

## Documentación

- `Docs/UML`: diagrama de clases
- `Docs/SQL`: modelo relacional (DER) y script del esquema
- `AGENTS.md`: estructura del proyecto, convenciones y estado actual
