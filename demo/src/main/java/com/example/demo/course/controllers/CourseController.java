package com.example.demo.course.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.model.Course;
import com.example.demo.course.services.CourseService;
import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.dto.CourseResponse;
import com.example.demo.enrollment.services.EnrollmentService;
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
        List<Course> cursos;
        if (studentId == null) {
            cursos = courseService.getAll();
        } else {
            UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ALUMNO, Rol.ADMINISTRADOR);
            actor.exigirPropio(studentId, "Solo puede ver sus propios cursos");
            cursos = enrollmentService.getCoursesOfStudent(studentId);
        }
        return cursos.stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return respuesta(courseService.getById(id));
    }

    // Solo instructores y administradores crean; el instructor crea los suyos siempre.
    @SecurityRequirement(name = "token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody CourseRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR);
        validar(solicitud, actor);
        return respuesta(courseService.create(solicitud, actor));
    }

    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody CourseRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR);
        validar(solicitud, actor);
        return respuesta(courseService.update(id, solicitud, actor));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR);
        courseService.delete(id, actor);
    }

    // instructorId solo lo pide el administrador: el instructor es el del token.
    private void validar(CourseRequest solicitud, UsuarioAutenticado actor) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getTitle(), "title");
        exigir(solicitud.getBasePrice(), "basePrice");
        exigir(solicitud.getStartDate(), "startDate");
        exigir(solicitud.getEndDate(), "endDate");
        exigir(solicitud.getMaxCapacity(), "maxCapacity");
        exigir(solicitud.getSpecialtyId(), "specialtyId");
        if (actor.rol() == Rol.ADMINISTRADOR) {
            exigir(solicitud.getInstructorId(), "instructorId");
        }
    }

    private CourseResponse respuesta(Course curso) {
        return new CourseResponse(curso.getId(), curso.getTitle(), curso.getDescription(), curso.getBasePrice(),
                curso.getStartDate(), curso.getEndDate(), curso.getMaxCapacity(), curso.getStatus(),
                curso.getSpecialty() == null ? null : curso.getSpecialty().getId(),
                curso.getInstructor() == null ? null : curso.getInstructor().getId());
    }
}
