package ar.edu.unnoba.pm2026.enrollment.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentCourseResponse;
import ar.edu.unnoba.pm2026.enrollment.service.StudentCatalogService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#42): el catalogo del alumno del token, con el precio que pagaria hoy.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/student/courses")
public class StudentCourseController {

    private final StudentCatalogService studentCatalogService;
    private final AuthorizationService authorizationService;

    public StudentCourseController(StudentCatalogService studentCatalogService,
            AuthorizationService authorizationService) {
        this.studentCatalogService = studentCatalogService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/available")
    public List<StudentCourseResponse> getAvailable(
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT);
        return studentCatalogService.getAvailableCourses(actor.id());
    }

    @GetMapping("/{id}")
    public StudentCourseResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT);
        return studentCatalogService.getCourse(actor.id(), id);
    }
}
