package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.enrollment.services.EnrollmentService;
import com.example.demo.user.dto.StudentRequest;
import com.example.demo.user.dto.StudentResponse;
import com.example.demo.user.model.Student;
import com.example.demo.user.services.StudentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    public StudentController(StudentService studentService, EnrollmentService enrollmentService) {
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<StudentResponse> getAll(@RequestParam(required = false) Long courseId) {
        List<Student> alumnos = courseId == null
                ? studentService.getAll()
                : enrollmentService.getStudentsOfCourse(courseId);
        return alumnos.stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        return respuesta(studentService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@RequestBody StudentRequest solicitud) {
        validar(solicitud);
        Student alumno = new Student(solicitud.getFirstName(), solicitud.getLastName(), solicitud.getEmail(),
                solicitud.getPassword(), solicitud.getDni());
        return respuesta(studentService.save(alumno));
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @RequestBody StudentRequest solicitud) {
        validar(solicitud);
        Student alumno = studentService.getById(id);
        alumno.setFirstName(solicitud.getFirstName());
        alumno.setLastName(solicitud.getLastName());
        alumno.setEmail(solicitud.getEmail());
        alumno.setPassword(solicitud.getPassword());
        alumno.setDni(solicitud.getDni());
        return respuesta(studentService.save(alumno));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
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
