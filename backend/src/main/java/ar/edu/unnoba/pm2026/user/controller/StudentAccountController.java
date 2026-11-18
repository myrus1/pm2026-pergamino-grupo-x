package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import ar.edu.unnoba.pm2026.user.dto.CreateStudentRequest;
import ar.edu.unnoba.pm2026.user.dto.StudentSummaryResponse;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#42): registro y login del alumno, los dos publicos. Conviven con POST /api/students y /student/auth.
@RestController
public class StudentAccountController {

    private final StudentService studentService;
    private final AuthenticationService authenticationService;

    public StudentAccountController(StudentService studentService, AuthenticationService authenticationService) {
        this.studentService = studentService;
        this.authenticationService = authenticationService;
    }

    // 409 si el email o el DNI ya existen (lo detecta la base)
    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StudentSummaryResponse> register(@RequestBody @Valid CreateStudentRequest request) {
        Student student = new Student(request.getName(), request.getLastName(), request.getEmail(),
                request.getPassword(), request.getDni());
        StudentSummaryResponse created = StudentSummaryResponse.from(studentService.create(student));
        return ResponseEntity.created(ResourceLocation.forPath("/api/students/{id}", created.id())).body(created);
    }

    // Login separado del alumno: {"token": "Bearer ..."} o 401
    @PostMapping(value = "/students/auth", produces = "application/json")
    public Map<String, String> authentication(@RequestBody @Valid AuthenticationRequest request) {
        Student credentials = new Student(null, null, request.email(), request.password(), null);
        return Map.of("token", authenticationService.authenticate(credentials));
    }
}
