package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.services.CourseService;
import com.example.demo.enrollment.services.EnrollmentService;
import com.example.demo.user.dto.StudentRequest;
import com.example.demo.user.dto.StudentResponse;
import com.example.demo.user.model.Student;
import com.example.demo.user.services.StudentService;
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

// API REST de alumnos: /api/students
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    public StudentController(StudentService studentService, EnrollmentService enrollmentService,
            CourseService courseService, AuthorizationService authorizationService) {
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    // Listar todos es de administrador; listar los de un curso tambien lo puede hacer
    // el instructor dueño de ese curso.
    @SecurityRequirement(name = "token")
    @GetMapping
    public List<StudentResponse> getAll(@RequestParam(required = false) Long courseId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        List<Student> alumnos;
        if (courseId == null) {
            authorizationService.autorizar(token, Rol.ADMINISTRADOR);
            alumnos = studentService.getAll();
        } else {
            UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ADMINISTRADOR, Rol.INSTRUCTOR);
            courseService.getById(courseId, actor);
            alumnos = enrollmentService.getStudentsOfCourse(courseId);
        }
        return alumnos.stream().map(this::respuesta).toList();
    }

    @SecurityRequirement(name = "token")
    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        return respuesta(studentService.getById(id));
    }

    // Registro publico: no pide token (los administradores se crean por /api/administrators).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@RequestBody StudentRequest solicitud) {
        validar(solicitud);
        Student alumno = new Student(solicitud.getFirstName(), solicitud.getLastName(), solicitud.getEmail(),
                solicitud.getPassword(), solicitud.getDni());
        return respuesta(studentService.create(alumno));
    }

    // El alumno no edita su perfil: lo hace un administrador.
    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @RequestBody StudentRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        validar(solicitud);
        Student alumno = studentService.getById(id);
        alumno.setFirstName(solicitud.getFirstName());
        alumno.setLastName(solicitud.getLastName());
        alumno.setEmail(solicitud.getEmail());
        alumno.setPassword(solicitud.getPassword());
        alumno.setDni(solicitud.getDni());
        return respuesta(studentService.update(alumno));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        studentService.getById(id);
        studentService.delete(id);
    }

    private void validar(StudentRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getFirstName(), "firstName");
        exigir(solicitud.getLastName(), "lastName");
        exigir(solicitud.getEmail(), "email");
        exigir(solicitud.getPassword(), "password");
        exigir(solicitud.getDni(), "dni");
    }

    private StudentResponse respuesta(Student alumno) {
        return new StudentResponse(alumno.getId(), alumno.getFirstName(), alumno.getLastName(), alumno.getEmail(),
                alumno.getDni());
    }
}
