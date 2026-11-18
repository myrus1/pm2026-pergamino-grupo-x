package ar.edu.unnoba.pm2026.enrollment.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrollmentRequest;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrollmentResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de inscripciones: /api/enrollments.
// No tiene PUT ni DELETE: una inscripcion se crea (con las reglas de Student.applyCourse) y no se cancela.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public EnrollmentController(EnrollmentService enrollmentService, AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // Cada rol ve solo lo que le corresponde: sus inscripciones o las de sus cursos.
    @GetMapping
    public List<EnrollmentResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT, Role.INSTRUCTOR,
                Role.ADMINISTRATOR);
        List<Enrollment> enrollments = switch (actor.role()) {
            case STUDENT -> enrollmentService.getAllByStudent(actor.id());
            case INSTRUCTOR -> enrollmentService.getAllByInstructor(actor.id());
            case ADMINISTRATOR -> enrollmentService.getAll();
        };
        return enrollments.stream().map(this::response).toList();
    }

    @GetMapping("/revenue")
    public RevenueResponse totalRevenue(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return new RevenueResponse(enrollmentService.totalRevenue());
    }

    @GetMapping("/{id}")
    public EnrollmentResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT, Role.INSTRUCTOR,
                Role.ADMINISTRATOR);
        return response(enrollmentService.getById(id, actor));
    }

    // Solo un alumno puede inscribirse, y solo a si mismo.
    @ApiResponse(responseCode = "404", description = "Alumno o curso no encontrados")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EnrollmentResponse> create(@RequestBody @Valid EnrollmentRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT);
        EnrollmentResponse created = response(enrollmentService.enroll(request.getStudentId(), request.getCourseId(), actor));
        return ResponseEntity.created(ResourceLocation.forId(created.getId())).body(created);
    }

    private EnrollmentResponse response(Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getId(), enrollment.getEnrollmentDate(),
                enrollment.getFinalAmount(),
                enrollment.getStudent() == null ? null : enrollment.getStudent().getId(),
                enrollment.getCourse() == null ? null : enrollment.getCourse().getId());
    }
}
