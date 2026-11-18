package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> authentication(@RequestBody @Valid AuthenticationRequest request) {
        Administrator credentials = new Administrator(null, null, request.email(), request.password());
        return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credentials)));
    }
}
