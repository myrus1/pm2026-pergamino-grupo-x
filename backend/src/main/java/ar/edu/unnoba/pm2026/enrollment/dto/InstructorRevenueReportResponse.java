package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// TP6: GET /instructor/revenue/enrollments: lo que le corresponde al instructor (65% del precio base de cada
// inscripcion) en el rango, con el detalle. fromDate/toDate son null si no se mandaron.
public record InstructorRevenueReportResponse(LocalDate fromDate, LocalDate toDate, BigDecimal total,
        List<EnrollmentRevenue> enrollments) {

    public record EnrollmentRevenue(Long enrollmentId, LocalDateTime enrollmentDate, Long courseId,
            String courseTitle, BigDecimal amount) {
    }
}
