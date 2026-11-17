# Login único (`POST /api/auth/token`)

El mismo endpoint autentica a alumnos, instructores y administradores. Busca el email en la
tabla base `users`, detecta el rol guardado en `User.rol` y firma el JWT con ese rol.

```mermaid
sequenceDiagram
    actor Cliente
    participant AuthController
    participant AuthenticationServiceImp
    participant UserRepository
    participant JwtTokenUtil

    Cliente->>AuthController: POST /api/auth/token {email, password}
    AuthController->>AuthController: validar email y password
    alt falta email o password
        AuthController-->>Cliente: 400 {"mensaje": "El campo '...' es obligatorio"}
    else credenciales completas
        AuthController->>AuthenticationServiceImp: authenticate(email, password)
        AuthenticationServiceImp->>UserRepository: findByEmail(email)
        UserRepository-->>AuthenticationServiceImp: User con rol STUDENT / INSTRUCTOR / ADMINISTRATOR
        alt usuario inexistente o password inválida
            AuthenticationServiceImp-->>AuthController: AuthenticationException
            AuthController-->>Cliente: 401 {"mensaje": "Email o contraseña incorrectos"}
        else credenciales válidas
            AuthenticationServiceImp->>JwtTokenUtil: generateToken(email, rol, userId)
            JwtTokenUtil-->>AuthenticationServiceImp: JWT (HMAC512, vence según jwt.expiration)
            AuthenticationServiceImp-->>AuthController: AuthenticationResult(token, rol, expiresIn)
            AuthController-->>Cliente: 200 {accessToken, tokenType: "Bearer", expiresIn, role}
        end
    end
```

## Uso del token

- El cliente arma el header `Authorization: Bearer <accessToken>`.
- En Swagger UI se pega ese mismo valor completo en el botón **Authorize**.
- En Postman la colección guarda `Bearer <accessToken>` en `tokenAdmin`, `tokenInstructor`, etc.
- Los logins viejos (`/student/auth`, `/instructor/auth`, `/administrator/auth`) siguen
  funcionando pero están deprecados y devuelven el token con el prefijo incluido.
