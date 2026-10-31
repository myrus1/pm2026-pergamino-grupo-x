package com.example.demo.enrollment.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.enrollment.dto.EnrollmentRequest;
import com.example.demo.enrollment.dto.EnrollmentResponse;
import com.example.demo.enrollment.dto.RevenueResponse;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.services.EnrollmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de inscripciones: /api/enrollments.
// No tiene PUT: una inscripcion se crea (con las reglas de Student.applyCourse) y se elimina.
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<EnrollmentResponse> getAll() {
        return enrollmentService.getAll().stream().map(this::respuesta).toList();
    }

    @GetMapping("/revenue")
    public RevenueResponse totalRevenue() {
        return new RevenueResponse(enrollmentService.totalRevenue());
    }

    @GetMapping("/{id}")
    public EnrollmentResponse getById(@PathVariable Long id) {
        return respuesta(enrollmentService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse create(@RequestBody EnrollmentRequest solicitud) {
        validar(solicitud);
        return respuesta(enrollmentService.enroll(solicitud.getStudentId(), solicitud.getCourseId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enrollmentService.getById(id);
        enrollmentService.delete(id);
    }

    private void validar(EnrollmentRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getStudentId(), "studentId");
        exigir(solicitud.getCourseId(), "courseId");
    }

    private EnrollmentResponse respuesta(Enrollment inscripcion) {
        return new EnrollmentResponse(inscripcion.getId(), inscripcion.getEnrollmentDate(),
                inscripcion.getFinalAmount(),
                inscripcion.getStudent() == null ? null : inscripcion.getStudent().getId(),
                inscripcion.getCourse() == null ? null : inscripcion.getCourse().getId());
    }
}
