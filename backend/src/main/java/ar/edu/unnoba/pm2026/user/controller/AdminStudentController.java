package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.user.dto.StudentDetailResponse;
import ar.edu.unnoba.pm2026.user.dto.StudentSummaryResponse;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#39): consultas de alumnos del administrador.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/admin/students")
public class AdminStudentController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public AdminStudentController(StudentService studentService, EnrollmentService enrollmentService,
            AuthorizationService authorizationService) {
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<StudentSummaryResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return studentService.getAll().stream().map(StudentSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StudentDetailResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return studentService.getDetail(id);
    }

    // Cursos en los que se inscribio, de la fecha de inicio mas nueva a la mas vieja
    @GetMapping("/{id}/courses")
    public List<CourseResponse> getCourses(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return enrollmentService.getCoursesOfStudent(id).stream().map(CourseResponse::from).toList();
    }
}
