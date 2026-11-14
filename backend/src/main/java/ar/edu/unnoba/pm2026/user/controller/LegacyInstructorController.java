package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.exception.ErrorResponse;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.user.dto.CreateInstructorRequest;
import ar.edu.unnoba.pm2026.user.dto.InstructorCourseResponse;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Recurso del instructor (TP4/TP5): registro, login y cursos propios.
// Los 401/403 los produce ApiExceptionHandler a partir de las excepciones de auth/.
@RestController
@RequestMapping("/instructor")
public class LegacyInstructorController {

    private final InstructorService instructorService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final CourseService courseService;

    public LegacyInstructorController(InstructorService instructorService, AuthenticationService authenticationService,
            AuthorizationService authorizationService, CourseService courseService) {
        this.instructorService = instructorService;
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.courseService = courseService;
    }

    @SecurityRequirement(name = "token")
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody CreateInstructorRequest request) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        Instructor instructor = new Instructor(request.firstName(), request.lastName(), request.email(),
                request.password());
        try {
            instructorService.create(instructor);
        } catch (InstructorEmailAlreadyExistsException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Ya existe un usuario con el email " + request.email()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<?> authentication(@RequestBody AuthenticationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.email(), "email");
        requireField(request.password(), "password");
        Instructor credentials = new Instructor(null, null, request.email(), request.password());
        return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credentials)));
    }

    @SecurityRequirement(name = "token")
    @GetMapping(value = {"", "/courses"}, produces = "application/json")
    public List<InstructorCourseResponse> getCourses(
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthenticatedUser authenticatedUser = authorizationService.authorize(token, Role.INSTRUCTOR);
        return courseService.getCoursesByInstructorId(authenticatedUser.id()).stream()
                .map(course -> new InstructorCourseResponse(course.getId(), course.getTitle(), course.getDescription()))
                .toList();
    }

    private void validate(CreateInstructorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.email(), "email");
        requireField(request.password(), "password");
        requireField(request.firstName(), "firstName");
        requireField(request.lastName(), "lastName");
    }
}
