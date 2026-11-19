package ar.edu.unnoba.pm2026.course.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @ApiResponse(responseCode = "401", description = "Autenticación requerida o inválida")
    @ApiResponse(responseCode = "403", description = "El alumno solicitado no coincide con el token")
    @ApiResponse(responseCode = "404", description = "Alumno no encontrado")
    @GetMapping
    public List<CourseResponse> getAll(@RequestParam(required = false) Long studentId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        List<Course> courses;
        if (studentId == null) {
            courses = courseService.getAll();
        } else {
            AuthenticatedUser actor = authorizationService.authorize(token, Role.STUDENT, Role.ADMINISTRATOR);
            actor.requireOwnership(studentId, ErrorCode.AUTHORIZATION_COURSES_OF_OTHERS);
            courses = enrollmentService.getCoursesOfStudent(studentId);
        }
        return courses.stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return response(courseService.getById(id));
    }

    // Crear y editar es solo del instructor, sobre sus cursos (TP6: el admin no hace acciones de instructor).
    @ApiResponse(responseCode = "404", description = "Instructor o especialidad no encontrados")
    @SecurityRequirement(name = "token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CourseResponse> create(@RequestBody @Valid CourseRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        CourseResponse created = response(courseService.create(request, actor));
        return ResponseEntity.created(ResourceLocation.forId(created.getId())).body(created);
    }

    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody @Valid CourseRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return response(courseService.update(id, request, actor));
    }

    // El instructor borra los suyos en borrador; el administrador puede sacar cualquier curso.
    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        courseService.delete(id, actor);
    }

    private CourseResponse response(Course course) {
        return CourseResponse.from(course);
    }
}
