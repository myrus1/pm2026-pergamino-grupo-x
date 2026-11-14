package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login del administrador: POST /administrator/auth -> {"token": "Bearer ..."} con rol ADMINISTRADOR.
@RestController
@RequestMapping("/administrator")
public class LegacyAdministratorController {

    private final AuthenticationService authenticationService;

    public LegacyAdministratorController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<?> authentication(@RequestBody AuthenticationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.email(), "email");
        requireField(request.password(), "password");
        Administrator credentials = new Administrator(null, null, request.email(), request.password());
        return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credentials)));
    }
}
