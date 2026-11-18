# syntax=docker/dockerfile:1

# Imagen del backend (Spring Boot). Se construye desde la RAÍZ del repo:
#   docker build -t pm2026-backend .
# Las variables de entorno NO se hornean en la imagen: se pasan en runtime
# con `docker run --env-file .env` (el .env está en .dockerignore).

# ---------------------------------------------------------------------------
# Etapa 1: build (Maven + JDK). Todo esto se descarta en la imagen final.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

# Maven toma por defecto 1/4 de la RAM del host; lo acotamos para que el build
# no se coma la memoria de la máquina (y sea predecible en CI).
ENV MAVEN_OPTS="-Xms256m -Xmx1024m -XX:MaxMetaspaceSize=384m"

WORKDIR /build

# Primero solo el pom: si no cambia, esta capa se reutiliza y no se vuelven a
# resolver las dependencias.
COPY backend/pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline

# Recién ahora el código: un cambio en src/ no invalida la descarga anterior.
COPY backend/src ./src

# -Dmaven.test.skip: Pm2026ApplicationTests es @SpringBootTest y necesita una
# PostgreSQL accesible; además así no se compilan las clases de test (menos
# tiempo y menos RAM de build).
RUN mvn -B -ntp -Dmaven.test.skip=true package

# ---------------------------------------------------------------------------
# Etapa 2: runtime (solo JRE + jar, sin Maven ni fuentes)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

# Usuario sin privilegios.
RUN addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app app

WORKDIR /app

# El glob matchea el jar repackageado (pm2026-backend-0.0.1-SNAPSHOT.jar) y no el
# .jar.original que deja spring-boot-maven-plugin.
COPY --from=build --chown=app:app /build/target/pm2026-backend-*.jar /app/app.jar

USER app

EXPOSE 8080

# Ajustes pensados para un contenedor chico:
#  - MaxRAMPercentage: el heap sigue el límite de memoria del contenedor
#    (docker run -m / compose mem_limit) en vez de la RAM del host.
#  - UseSerialGC: menos overhead de memoria que G1 con heaps chicos.
#  - MaxMetaspaceSize: acota el metaspace para que no crezca sin techo.
#  - ExitOnOutOfMemoryError: el proceso muere y el orquestador lo reinicia
#    en vez de quedar zombi.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60.0 -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# `exec` para que la JVM sea el PID 1 y reciba SIGTERM (apagado ordenado).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
