# Programación Multiplataforma 2026 - Pergamino

Proyecto desarrollado para la materia **Programación Multiplataforma 2026**.

## Integrantes

- Buccolini
- Mendez
- Rothamel

## Requisitos

- Java 21 o superior
- PostgreSQL

## Configuración

### Variables del archivo `.env`

```env
DB_URL=jdbc:postgresql://localhost:5432/mydb
DB_USERNAME=user    
DB_PASSWORD=pass
```



## Ejecución

Desde la raíz del repositorio, ejecutá:

```bash
cd proyecto-pm-2026
set -a && . .env && set +a
./mvnw spring-boot:run
```

La aplicación se inicia en [http://localhost:8080](http://localhost:8080).



