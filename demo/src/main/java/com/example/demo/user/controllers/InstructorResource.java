package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.ErrorResponse;
import com.example.demo.course.services.CourseService;
import com.example.demo.user.dto.CreateInstructorRequestDTO;
import com.example.demo.user.dto.InstructorAuthenticationRequestDTO;
import com.example.demo.user.dto.InstructorCoursesResponseDTO;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
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

@RestController
@RequestMapping("/instructor")
public class InstructorResource {

    private final InstructorService instructorService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final CourseService courseService;

    public InstructorResource(InstructorService instructorService, AuthenticationService authenticationService,
            AuthorizationService authorizationService, CourseService courseService) {
        this.instructorService = instructorService;
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateInstructorRequestDTO solicitud) {
        validar(solicitud);
        Instructor instructor = new Instructor(solicitud.firstName(), solicitud.lastName(), solicitud.email(),
                solicitud.password());
        try {
            instructorService.create(instructor);
        } catch (InstructorEmailAlreadyExistsException excepcion) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(excepcion.getMessage()));
        } catch (DataIntegrityViolationException excepcion) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Ya existe un usuario con el email " + solicitud.email()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/auth", produces = "application/json")
    public ResponseEntity<?> authentication(@RequestBody InstructorAuthenticationRequestDTO solicitud)
            throws Exception {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.email(), "email");
        exigir(solicitud.password(), "password");
        Instructor credenciales = new Instructor(null, null, solicitud.email(), solicitud.password());
        try {
            return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credenciales)));
        } catch (AuthenticationException excepcion) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(excepcion.getMessage()));
        }
    }

    @GetMapping(value = {"", "/courses"}, produces = "application/json")
    public ResponseEntity<?> getCourses(@RequestHeader(value = "Authorization", required = false) String token)
            throws Exception {
        Instructor instructor;
        try {
            instructor = authorizationService.authorize(token);
        } catch (AuthorizationException excepcion) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(excepcion.getMessage()));
        }
        List<InstructorCoursesResponseDTO> cursos = courseService.getCoursesByInstructorId(instructor.getId()).stream()
                .map(curso -> new InstructorCoursesResponseDTO(curso.getId(), curso.getTitle(), curso.getDescription()))
                .toList();
        return ResponseEntity.ok(cursos);
    }

    private void validar(CreateInstructorRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.email(), "email");
        exigir(solicitud.password(), "password");
        exigir(solicitud.firstName(), "firstName");
        exigir(solicitud.lastName(), "lastName");
    }
}
