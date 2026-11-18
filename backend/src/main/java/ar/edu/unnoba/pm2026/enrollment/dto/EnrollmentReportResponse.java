package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TP6: GET /admin/reports/enrollments: cada inscripcion del rango con el precio base (el actual del curso),
// el precio final que pago el alumno y la diferencia (descuento de fidelidad).
public record EnrollmentReportResponse(Long enrollmentId, LocalDateTime enrollmentDate, Long studentId,
        String studentName, String studentLastName, Long courseId, String courseTitle, BigDecimal basePrice,
        BigDecimal finalAmount, BigDecimal discount) {
}
