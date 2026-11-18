package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TP6: un alumno inscripto en un curso, con la fecha de inscripcion y el precio final que pago.
// El alumno es LAZY: se arma dentro de una transaccion (EnrollmentService).
public record EnrolledStudentResponse(Long enrollmentId, Long studentId, String name, String lastName,
        String email, String dni, LocalDateTime enrollmentDate, BigDecimal finalAmount) {
}
