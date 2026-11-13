package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.user.dto.AuthenticationRequestDTO;
import com.example.demo.user.model.Student;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login del alumno: POST /student/auth -> {"token": "Bearer ..."} con rol ALUMNO.
@RestController
@RequestMapping("/student")
public class StudentResource {

    private final AuthenticationService authenticationService;

    public StudentResource(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

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
