package com.example.demo.enrollment.dto;

import lombok.Getter;
import lombok.Setter;

// Datos de entrada para inscribir a un alumno en un curso.
// Se valida y ejecuta en Student.applyCourse, nunca se crea una inscripcion directa.
@Getter
@Setter
public class EnrollmentRequest {

    private Long studentId;
    private Long courseId;
}
