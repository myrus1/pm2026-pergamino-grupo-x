package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.ErrorResponse;
import com.example.demo.common.Rol;
import com.example.demo.course.services.CourseService;
import com.example.demo.user.dto.AuthenticationRequestDTO;
import com.example.demo.user.dto.CreateInstructorRequestDTO;
import com.example.demo.user.dto.InstructorCoursesResponseDTO;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
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

    @SecurityRequirement(name = "token")
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody CreateInstructorRequestDTO solicitud) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
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
    public ResponseEntity<?> authentication(@RequestBody AuthenticationRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.email(), "email");
        exigir(solicitud.password(), "password");
        Instructor credenciales = new Instructor(null, null, solicitud.email(), solicitud.password());
        return ResponseEntity.ok(Map.of("token", authenticationService.authenticate(credenciales)));
    }

    @SecurityRequirement(name = "token")
    @GetMapping(value = {"", "/courses"}, produces = "application/json")
    public List<InstructorCoursesResponseDTO> getCourses(
            @RequestHeader(value = "Authorization", required = false) String token) {
        UsuarioAutenticado usuario = authorizationService.autorizar(token, Rol.INSTRUCTOR);
        return courseService.getCoursesByInstructorId(usuario.id()).stream()
                .map(curso -> new InstructorCoursesResponseDTO(curso.getId(), curso.getTitle(), curso.getDescription()))
                .toList();
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
