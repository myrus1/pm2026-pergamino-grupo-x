package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// TP6: GET /admin/reports/revenue: lo que pagaron los alumnos en el rango (precio final de cada inscripcion)
public record RevenueReportResponse(LocalDate fromDate, LocalDate toDate, int enrollments, BigDecimal total) {
}
