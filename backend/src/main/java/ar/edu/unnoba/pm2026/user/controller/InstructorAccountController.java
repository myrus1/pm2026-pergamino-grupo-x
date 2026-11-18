package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import ar.edu.unnoba.pm2026.user.dto.CreateInstructorRequest;
import ar.edu.unnoba.pm2026.user.dto.InstructorAccountResponse;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#41): registro y login del instructor. Los dos son publicos: el TP6 no pide token para registrarse.
// El alta con token de admin sigue en POST /api/instructors, y el login del TP5 en POST /instructor/auth.
@RestController
public class InstructorAccountController {

    private final InstructorService instructorService;
    private final AuthenticationService authenticationService;

    public InstructorAccountController(InstructorService instructorService,
            AuthenticationService authenticationService) {
        this.instructorService = instructorService;
        this.authenticationService = authenticationService;
    }

    // 409 si ya existe un instructor (o cualquier usuario) con ese email
    @PostMapping("/instructor")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InstructorAccountResponse> register(@RequestBody @Valid CreateInstructorRequest request) {
        Instructor instructor = new Instructor(request.nameOrEmpty(), request.lastNameOrEmpty(), request.email(),
                request.password());
        InstructorAccountResponse created = InstructorAccountResponse.from(instructorService.create(instructor));
        return ResponseEntity.created(ResourceLocation.forPath("/api/instructors/{id}", created.id())).body(created);
    }

    // Login separado del instructor: {"token": "Bearer ..."} o 401
    @PostMapping(value = "/auth", produces = "application/json")
    public Map<String, String> authentication(@RequestBody @Valid AuthenticationRequest request) {
        Instructor credentials = new Instructor(null, null, request.email(), request.password());
        return Map.of("token", authenticationService.authenticate(credentials));
    }
}
