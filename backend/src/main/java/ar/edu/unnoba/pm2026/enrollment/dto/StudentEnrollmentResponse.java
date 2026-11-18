package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// TP6: una inscripcion del alumno con el curso, el precio base, lo que pago y el descuento que se le aplico.
// Recorre el curso (LAZY): se arma dentro de una transaccion (EnrollmentService).
public record StudentEnrollmentResponse(Long enrollmentId, LocalDateTime enrollmentDate, BigDecimal basePrice,
        BigDecimal finalAmount, BigDecimal discount, EnrolledCourse course) {

    public record EnrolledCourse(Long id, String title, String description, LocalDate startDate, LocalDate endDate,
            String specialty, String instructor) {
    }
}
