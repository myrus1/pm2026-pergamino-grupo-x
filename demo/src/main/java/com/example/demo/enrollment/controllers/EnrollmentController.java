package com.example.demo.enrollment.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.enrollment.dto.EnrollmentRequest;
import com.example.demo.enrollment.dto.EnrollmentResponse;
import com.example.demo.enrollment.dto.RevenueResponse;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.services.EnrollmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de inscripciones: /api/enrollments.
// No tiene PUT ni DELETE: una inscripcion se crea (con las reglas de Student.applyCourse) y no se cancela.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public EnrollmentController(EnrollmentService enrollmentService, AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    // Cada rol ve solo lo que le corresponde: sus inscripciones o las de sus cursos.
    @GetMapping
    public List<EnrollmentResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ALUMNO, Rol.INSTRUCTOR,
                Rol.ADMINISTRADOR);
        List<Enrollment> inscripciones = switch (actor.rol()) {
            case ALUMNO -> enrollmentService.getAllDeAlumno(actor.id());
            case INSTRUCTOR -> enrollmentService.getAllDeInstructor(actor.id());
            case ADMINISTRADOR -> enrollmentService.getAll();
        };
        return inscripciones.stream().map(this::respuesta).toList();
    }

    @GetMapping("/revenue")
    public RevenueResponse totalRevenue(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        return new RevenueResponse(enrollmentService.totalRevenue());
    }

    @GetMapping("/{id}")
    public EnrollmentResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ALUMNO, Rol.INSTRUCTOR,
                Rol.ADMINISTRADOR);
        return respuesta(enrollmentService.getById(id, actor));
    }

    // Solo un alumno puede inscribirse, y solo a si mismo.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse create(@RequestBody EnrollmentRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ALUMNO, Rol.ADMINISTRADOR);
        validar(solicitud, actor);
        return respuesta(enrollmentService.enroll(solicitud.getStudentId(), solicitud.getCourseId()));
    }

    private void validar(EnrollmentRequest solicitud, UsuarioAutenticado actor) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getStudentId(), "studentId");
        exigir(solicitud.getCourseId(), "courseId");
        actor.exigirPropio(solicitud.getStudentId(), "Solo puede inscribirse a si mismo");
    }

    private EnrollmentResponse respuesta(Enrollment inscripcion) {
        return new EnrollmentResponse(inscripcion.getId(), inscripcion.getEnrollmentDate(),
                inscripcion.getFinalAmount(),
                inscripcion.getStudent() == null ? null : inscripcion.getStudent().getId(),
                inscripcion.getCourse() == null ? null : inscripcion.getCourse().getId());
    }
}
