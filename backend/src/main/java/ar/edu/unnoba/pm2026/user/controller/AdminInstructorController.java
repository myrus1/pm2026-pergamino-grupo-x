package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.user.dto.InstructorDetailResponse;
import ar.edu.unnoba.pm2026.user.dto.InstructorSummaryResponse;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#39): consultas de instructores del administrador.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/admin/instructors")
public class AdminInstructorController {

    private final InstructorService instructorService;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    public AdminInstructorController(InstructorService instructorService, CourseService courseService,
            AuthorizationService authorizationService) {
        this.instructorService = instructorService;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<InstructorSummaryResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return instructorService.getSummaries();
    }

    @GetMapping("/{id}")
    public InstructorDetailResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return instructorService.getDetail(id);
    }

    // Cursos aprobados (publicados) que dicta el instructor
    @GetMapping("/{id}/courses")
    public List<CourseResponse> getApprovedCourses(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return courses(id, CourseStatus.PUBLISHED);
    }

    @GetMapping("/{id}/courses/pending")
    public List<CourseResponse> getPendingCourses(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return courses(id, CourseStatus.PENDING_APPROVAL);
    }

    private List<CourseResponse> courses(Long instructorId, CourseStatus status) {
        return courseService.getByInstructorAndStatus(instructorId, status).stream()
                .map(CourseResponse::from)
                .toList();
    }
}
