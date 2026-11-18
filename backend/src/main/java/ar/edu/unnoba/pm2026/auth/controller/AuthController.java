package ar.edu.unnoba.pm2026.auth.controller;

import ar.edu.unnoba.pm2026.auth.dto.AuthTokenResponse;
import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Login unico (#27): el mismo endpoint para alumnos, instructores y administradores. Convive con los
// logins por rol del TP6 (/students/auth, /auth, /admin/auth) y con los viejos, que quedan deprecados.
// Los 400 y 401 salen de ApiExceptionHandler en formato ProblemDetail.
@RestController
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Obtener un token JWT",
            description = "Login unico para alumnos, instructores y administradores. Busca el email en la "
                    + "tabla users y devuelve el token con el rol del usuario.")
    // Ruta completa en el metodo: OpenApiConfig la reconoce como login por el sufijo /auth/token
    @PostMapping(value = "/api/auth/token", produces = "application/json")
    public AuthTokenResponse token(@RequestBody @Valid AuthenticationRequest request) {
        return AuthTokenResponse.from(authenticationService.authenticate(request.email(), request.password()));
    }
}
