package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.user.dto.InstructorRequest;
import ar.edu.unnoba.pm2026.user.dto.InstructorResponse;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de instructores: /api/instructors (y asignacion de especialidades)
@RestController
@RequestMapping("/api/instructors")
public class InstructorController {

    private final InstructorService instructorService;
    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public InstructorController(InstructorService instructorService, EnrollmentService enrollmentService,
            AuthorizationService authorizationService) {
        this.instructorService = instructorService;
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // Listar instructores es publico (hace falta para mostrar los cursos).
    @GetMapping
    public List<InstructorResponse> getAll() {
        return instructorService.getAll().stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    public InstructorResponse getById(@PathVariable Long id) {
        return response(instructorService.getById(id));
    }

    @GetMapping("/{id}/specialties")
    public List<SpecialtyResponse> getSpecialties(@PathVariable Long id) {
        return instructorService.getSpecialties(id);
    }

    // La recaudacion es del propio instructor (o de un administrador).
    @SecurityRequirement(name = "token")
    @GetMapping("/{id}/revenue")
    public RevenueResponse revenue(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        actor.requireOwnership(id, "La recaudacion no le pertenece");
        return new RevenueResponse(enrollmentService.instructorRevenue(id));
    }

    // Crear instructores es de administrador (el registro abierto es solo de alumnos).
    @SecurityRequirement(name = "token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorResponse create(@RequestBody InstructorRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        Instructor instructor = new Instructor(request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPassword());
        return response(instructorService.create(instructor));
    }

    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public InstructorResponse update(@PathVariable Long id, @RequestBody InstructorRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        actor.requireOwnership(id, "Solo puede modificar su propio perfil");
        validate(request);
        Instructor instructor = instructorService.getById(id);
        instructor.setFirstName(request.getFirstName());
        instructor.setLastName(request.getLastName());
        instructor.setEmail(request.getEmail());
        instructor.setPassword(request.getPassword());
        return response(instructorService.update(instructor));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        instructorService.getById(id);
        instructorService.delete(id);
    }

    // Las especialidades las administra solo el administrador.
    @SecurityRequirement(name = "token")
    @PutMapping("/{id}/specialties/{specialtyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addSpecialty(@PathVariable Long id, @PathVariable Long specialtyId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        instructorService.addSpecialty(id, specialtyId);
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}/specialties/{specialtyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSpecialty(@PathVariable Long id, @PathVariable Long specialtyId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        instructorService.removeSpecialty(id, specialtyId);
    }

    private void validate(InstructorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.getFirstName(), "firstName");
        requireField(request.getLastName(), "lastName");
        requireField(request.getEmail(), "email");
        requireField(request.getPassword(), "password");
    }

    private InstructorResponse response(Instructor instructor) {
        return new InstructorResponse(instructor.getId(), instructor.getFirstName(), instructor.getLastName(),
                instructor.getEmail());
    }
}
