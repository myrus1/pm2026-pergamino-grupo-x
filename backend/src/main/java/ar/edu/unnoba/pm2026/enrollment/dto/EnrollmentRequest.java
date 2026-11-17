package ar.edu.unnoba.pm2026.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para inscribir a un alumno en un curso.
// Se valida y ejecuta en Student.applyCourse, nunca se crea una inscripcion directa.
@Getter
@Setter
public class EnrollmentRequest {

    @NotNull
    private Long studentId;
    @NotNull
    private Long courseId;
}
