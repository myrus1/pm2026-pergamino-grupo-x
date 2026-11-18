package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;

// TP6: GET /admin/reports/courses-revenue: por curso, inscriptos y total facturado en el rango
public record CourseRevenueResponse(Long courseId, String title, int enrollments, BigDecimal total) {
}
