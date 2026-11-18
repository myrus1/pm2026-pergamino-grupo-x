package ar.edu.unnoba.pm2026.course.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrolledStudentResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#39): consultas de cursos del administrador.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public AdminCourseController(CourseService courseService, EnrollmentService enrollmentService,
            AuthorizationService authorizationService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // Cursos aprobados (publicados), de la fecha de inicio mas nueva a la mas vieja
    @GetMapping
    public List<CourseResponse> getApproved(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return courseService.getPublished().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CourseDetailResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return courseService.getDetail(id);
    }

    @GetMapping("/{id}/students")
    public List<EnrolledStudentResponse> getStudents(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return enrollmentService.getEnrolledStudents(id);
    }
}
