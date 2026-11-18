# Login único (`POST /api/auth/token`)

El mismo endpoint autentica a alumnos, instructores y administradores. Busca el email en la
tabla base `users` (con la herencia JOINED vuelve el `Student`, `Instructor` o `Administrator`), toma el rol de
`User.getRole()` y firma el JWT con ese rol. No hay columna `rol` en la base.

```mermaid
sequenceDiagram
    actor Cliente
    participant AuthController
    participant AuthenticationServiceImpl
    participant UserRepository
    participant JwtTokenUtil

    Cliente->>AuthController: POST /api/auth/token {email, password}
    AuthController->>AuthController: @Valid AuthenticationRequest
    alt falta email o password, o el email no es valido
        AuthController-->>Cliente: 400 ProblemDetail ("El campo '...' es obligatorio")
    else credenciales completas
        AuthController->>AuthenticationServiceImpl: authenticate(email, password)
        AuthenticationServiceImpl->>UserRepository: findByEmail(email)
        UserRepository-->>AuthenticationServiceImpl: Student / Instructor / Administrator
        alt usuario inexistente o password inválida
            AuthenticationServiceImpl-->>AuthController: AuthenticationException
            AuthController-->>Cliente: 401 ProblemDetail ("Email o contraseña incorrectos")
        else credenciales válidas
            AuthenticationServiceImpl->>JwtTokenUtil: generateRawToken(email, role, userId)
            JwtTokenUtil-->>AuthenticationServiceImpl: JWT (HMAC512, vence según jwt.expiration)
            AuthenticationServiceImpl-->>AuthController: AuthenticationResult(token, role, expiresIn)
            AuthController-->>Cliente: 200 {accessToken, tokenType: "Bearer", expiresIn, role}
        end
    end
```

## Uso del token

- El cliente arma el header `Authorization: Bearer <accessToken>`.
- En Swagger UI se pega ese mismo valor completo en el botón **Authorize**.
- En Postman la colección guarda `Bearer <accessToken>` en `tokenAdmin`, `tokenInstructor`, etc.
- El claim `rol` del token lleva `ALUMNO`, `INSTRUCTOR` o `ADMINISTRADOR` (lo que lee el front); el campo
  `role` de la respuesta va en inglés.
- Los logins por rol del TP6 (`/students/auth`, `/auth`, `/admin/auth`) siguen igual. Los viejos
  (`/student/auth`, `/instructor/auth`, `/administrator/auth`) siguen funcionando pero están deprecados. Todos
  devuelven el token con el prefijo incluido.
