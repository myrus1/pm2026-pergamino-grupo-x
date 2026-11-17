package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.user.dto.CreateInstructorRequest;
import ar.edu.unnoba.pm2026.user.dto.InstructorCourseResponse;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody @Valid CreateInstructorRequest request) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        Instructor instructor = new Instructor(request.firstName(), request.lastName(), request.email(),
                request.password());
        instructorService.create(instructor);
    }

    @PostMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<?> authentication(@RequestBody @Valid AuthenticationRequest request) {
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

}
