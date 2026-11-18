package ar.edu.unnoba.pm2026.course.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.course.dto.UpdateCourseRequest;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrolledStudentResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import ar.edu.unnoba.pm2026.user.dto.InstructorCourseResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#41): los cursos del instructor. Todo pide token de instructor (un admin recibe 403) y solo opera
// sobre sus cursos: uno ajeno es 403, y editar o borrar uno que ya salio de borrador es 409.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/instructor/courses")
public class InstructorCourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public InstructorCourseController(CourseService courseService, EnrollmentService enrollmentService,
            AuthorizationService authorizationService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // Lista de InstructorCoursesResponseDTO del TP6: id, name y description
    @GetMapping(produces = "application/json")
    public List<InstructorCourseResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return courseService.getCoursesByInstructorId(actor.id()).stream()
                .map(InstructorCourseResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseDetailResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return courseService.getDetail(id, actor);
    }

    // El curso nace en DRAFT y a nombre del instructor del token
    @ApiResponse(responseCode = "404", description = "Especialidad no encontrada")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CourseResponse> create(@RequestBody @Valid CourseRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        CourseResponse created = CourseResponse.from(courseService.create(request, actor));
        return ResponseEntity.created(ResourceLocation.forId(created.getId())).body(created);
    }

    // Solo los campos que vienen en el body
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody @Valid UpdateCourseRequest changes,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return CourseResponse.from(courseService.updatePartially(id, changes, actor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        courseService.delete(id, actor);
    }

    @GetMapping("/{id}/students")
    public List<EnrolledStudentResponse> getStudents(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return enrollmentService.getEnrolledStudents(id, actor);
    }
}
