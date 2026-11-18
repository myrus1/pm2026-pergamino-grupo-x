package ar.edu.unnoba.pm2026.user.dto;

import java.math.BigDecimal;

// TP6: GET /admin/students/{id}: datos del alumno, en cuantos cursos se inscribio y cuanto pago en total.
// Las inscripciones son LAZY: se arma dentro de una transaccion (StudentService).
public record StudentDetailResponse(Long id, String name, String lastName, String email, String dni,
        int enrolledCourses, BigDecimal totalPaid) {
}
