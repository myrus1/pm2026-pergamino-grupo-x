package com.example.demo.auth.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.dto.AuthTokenResponse;
import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.user.dto.AuthenticationRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login unico: busca el email en users, detecta el rol del usuario y devuelve el JWT.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Obtener un token JWT",
            description = "Login unico para alumnos, instructores y administradores. "
                    + "Busca el email en la tabla users y devuelve el token con el rol del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales validas: devuelve accessToken, tokenType, expiresIn y role"),
            @ApiResponse(responseCode = "400", description = "Falta email o password"),
            @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos")
    })
    @PostMapping(value = "/token", produces = "application/json")
    public ResponseEntity<AuthTokenResponse> token(@RequestBody AuthenticationRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.email(), "email");
        exigir(solicitud.password(), "password");
        return ResponseEntity.ok(AuthTokenResponse.desde(
                authenticationService.authenticate(solicitud.email(), solicitud.password())));
    }
}
