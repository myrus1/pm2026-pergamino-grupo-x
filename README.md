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

Las tablas se crean y actualizan solas al levantar la app (`spring.jpa.hibernate.ddl-auto=update`).

## Ejecución

Desde la raíz del repositorio (en Git Bash):

```bash
set -a && . ./.env && set +a
cd demo
./mvnw spring-boot:run
```

La aplicación se inicia en [http://localhost:8080](http://localhost:8080). Los endpoints están bajo `/api` (por ejemplo `/api/courses`).

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
