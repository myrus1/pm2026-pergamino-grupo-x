package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login viejo del administrador (deprecado): POST /administrator/auth -> {"token": "Bearer ..."}.
// Lo reemplazan el login unico POST /api/auth/token y el del TP6 POST /admin/auth.
@RestController
@RequestMapping("/administrator")
public class LegacyAdministratorController {

    private final AuthenticationService authenticationService;

    public LegacyAdministratorController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Deprecated
    @Operation(deprecated = true, description = "Deprecado: usar POST /api/auth/token")
    @PostMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<?> authentication(@RequestBody @Valid AuthenticationRequest request) {
        Administrator credentials = new Administrator(null, null, request.email(), request.password());
        return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credentials)));
    }
}
