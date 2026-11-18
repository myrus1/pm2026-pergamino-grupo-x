package ar.edu.unnoba.pm2026.enrollment.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentEnrollmentRequest;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentEnrollmentResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#42): el alumno del token se inscribe y consulta sus inscripciones.
@SecurityRequirement(name = "token")
@RestController
public class StudentEnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public StudentEnrollmentController(EnrollmentService enrollmentService,
            AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // 409 si no hay cupo, si el curso ya empezo o si ya esta inscripto
    @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    @PostMapping("/student/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StudentEnrollmentResponse> enroll(@RequestBody @Valid StudentEnrollmentRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT);
        StudentEnrollmentResponse created = enrollmentService.enrollStudent(request.getCourseId(), actor);
        // La inscripcion se consulta en /api/enrollments/{id} (el alumno ve las suyas)
        return ResponseEntity.created(ResourceLocation.forPath("/api/enrollments/{id}", created.enrollmentId()))
                .body(created);
    }

    @GetMapping("/students/enrollments")
    public List<StudentEnrollmentResponse> getMine(
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT);
        return enrollmentService.getStudentEnrollments(actor.id());
    }
}
