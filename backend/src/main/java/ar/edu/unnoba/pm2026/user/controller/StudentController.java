package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.user.dto.StudentRequest;
import ar.edu.unnoba.pm2026.user.dto.StudentResponse;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.StudentService;
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
        List<Student> students;
        if (courseId == null) {
            authorizationService.authorize(token, Role.ADMINISTRATOR);
            students = studentService.getAll();
        } else {
            AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR, Role.INSTRUCTOR);
            courseService.getById(courseId, actor);
            students = enrollmentService.getStudentsOfCourse(courseId);
        }
        return students.stream().map(this::response).toList();
    }

    @SecurityRequirement(name = "token")
    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return response(studentService.getById(id));
    }

    // Registro publico: no pide token (los administradores se crean por /api/administrators).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@RequestBody StudentRequest request) {
        validate(request);
        Student student = new Student(request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPassword(), request.getDni());
        return response(studentService.create(student));
    }

    // El alumno no edita su perfil: lo hace un administrador.
    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @RequestBody StudentRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        Student student = studentService.getById(id);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPassword(request.getPassword());
        student.setDni(request.getDni());
        return response(studentService.update(student));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        studentService.getById(id);
        studentService.delete(id);
    }

    private void validate(StudentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.getFirstName(), "firstName");
        requireField(request.getLastName(), "lastName");
        requireField(request.getEmail(), "email");
        requireField(request.getPassword(), "password");
        requireField(request.getDni(), "dni");
    }

    private StudentResponse response(Student student) {
        return new StudentResponse(student.getId(), student.getFirstName(), student.getLastName(), student.getEmail(),
                student.getDni());
    }
}
