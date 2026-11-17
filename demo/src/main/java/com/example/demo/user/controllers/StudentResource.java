package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.user.dto.AuthenticationRequestDTO;
import com.example.demo.user.model.Student;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login viejo del alumno (deprecado): POST /student/auth -> {"token": "Bearer ..."} con rol STUDENT.
// El login unico es POST /api/auth/token.
@RestController
@RequestMapping("/student")
public class StudentResource {

    private final AuthenticationService authenticationService;

    public StudentResource(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Deprecated
    @Operation(deprecated = true, description = "Deprecado: usar POST /api/auth/token")
    @PostMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<?> authentication(@RequestBody AuthenticationRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.email(), "email");
        exigir(solicitud.password(), "password");
        Student credenciales = new Student(null, null, solicitud.email(), solicitud.password(), null);
        return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credenciales)));
    }
}
