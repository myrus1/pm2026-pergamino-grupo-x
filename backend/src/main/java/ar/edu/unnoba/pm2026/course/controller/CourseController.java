package ar.edu.unnoba.pm2026.course.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de cursos: /api/courses
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public CourseController(CourseService courseService, EnrollmentService enrollmentService,
            AuthorizationService authorizationService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // El listado es publico; con ?studentId= solo lo puede pedir ese alumno o un administrador.
    @GetMapping
    public List<CourseResponse> getAll(@RequestParam(required = false) Long studentId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        List<Course> courses;
        if (studentId == null) {
            courses = courseService.getAll();
        } else {
            AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT, Role.ADMINISTRATOR);
            actor.requireOwnership(studentId, "Solo puede ver sus propios cursos");
            courses = enrollmentService.getCoursesOfStudent(studentId);
        }
        return courses.stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return response(courseService.getById(id));
    }

    // Solo instructores y administradores crean; el instructor crea los suyos siempre.
    @SecurityRequirement(name = "token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody CourseRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        validate(request, actor);
        return response(courseService.create(request, actor));
    }

    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody CourseRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        validate(request, actor);
        return response(courseService.update(id, request, actor));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        courseService.delete(id, actor);
    }

    // instructorId solo lo pide el administrador: el instructor es el del token.
    private void validate(CourseRequest request, AuthenticatedUser actor) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.getTitle(), "title");
        requireField(request.getBasePrice(), "basePrice");
        requireField(request.getStartDate(), "startDate");
        requireField(request.getEndDate(), "endDate");
        requireField(request.getMaxCapacity(), "maxCapacity");
        requireField(request.getSpecialtyId(), "specialtyId");
        if (actor.role() == Role.ADMINISTRATOR) {
            requireField(request.getInstructorId(), "instructorId");
        }
    }

    private CourseResponse response(Course course) {
        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(), course.getBasePrice(),
                course.getStartDate(), course.getEndDate(), course.getMaxCapacity(), course.getStatus(),
                course.getSpecialty() == null ? null : course.getSpecialty().getId(),
                course.getInstructor() == null ? null : course.getInstructor().getId());
    }
}
