package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.dto.SpecialtyResponse;
import com.example.demo.enrollment.dto.RevenueResponse;
import com.example.demo.enrollment.services.EnrollmentService;
import com.example.demo.user.dto.InstructorRequest;
import com.example.demo.user.dto.InstructorResponse;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
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
        return instructorService.getAll().stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public InstructorResponse getById(@PathVariable Long id) {
        return respuesta(instructorService.getById(id));
    }

    @GetMapping("/{id}/specialties")
    public List<SpecialtyResponse> getSpecialties(@PathVariable Long id) {
        return instructorService.getSpecialties(id);
    }

    @GetMapping("/{id}/revenue")
    public RevenueResponse revenue(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR);
        actor.exigirPropio(id, "La recaudacion no le pertenece");
        return new RevenueResponse(enrollmentService.instructorRevenue(id));
    }

    // Crear instructores es de administrador (el registro abierto es solo de alumnos).
    @SecurityRequirement(name = "token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorResponse create(@RequestBody InstructorRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        validar(solicitud);
        Instructor instructor = new Instructor(solicitud.getFirstName(), solicitud.getLastName(),
                solicitud.getEmail(), solicitud.getPassword());
        return respuesta(instructorService.create(instructor));
    }

    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public InstructorResponse update(@PathVariable Long id, @RequestBody InstructorRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR);
        actor.exigirPropio(id, "Solo puede modificar su propio perfil");
        validar(solicitud);
        Instructor instructor = instructorService.getById(id);
        instructor.setFirstName(solicitud.getFirstName());
        instructor.setLastName(solicitud.getLastName());
        instructor.setEmail(solicitud.getEmail());
        instructor.setPassword(solicitud.getPassword());
        return respuesta(instructorService.update(instructor));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        instructorService.getById(id);
        instructorService.delete(id);
    }

    // Las especialidades las administra solo el administrador.
    @SecurityRequirement(name = "token")
    @PutMapping("/{id}/specialties/{specialtyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignarEspecialidad(@PathVariable Long id, @PathVariable Long specialtyId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        instructorService.addSpecialty(id, specialtyId);
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}/specialties/{specialtyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarEspecialidad(@PathVariable Long id, @PathVariable Long specialtyId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        instructorService.removeSpecialty(id, specialtyId);
    }

    private void validar(InstructorRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getFirstName(), "firstName");
        exigir(solicitud.getLastName(), "lastName");
        exigir(solicitud.getEmail(), "email");
        exigir(solicitud.getPassword(), "password");
    }

    private InstructorResponse respuesta(Instructor instructor) {
        return new InstructorResponse(instructor.getId(), instructor.getFirstName(), instructor.getLastName(),
                instructor.getEmail());
    }
}
