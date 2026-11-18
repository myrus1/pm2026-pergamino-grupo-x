package ar.edu.unnoba.pm2026.course.dto;

import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

// TP6: detalle de un curso con su instructor, cuantos inscriptos tiene y cuantos lugares le quedan.
// Recorre relaciones LAZY: se arma dentro de una transaccion (CourseService).
public record CourseDetailResponse(Long id, String title, String description, CourseStatus status,
        LocalDate startDate, LocalDate endDate, BigDecimal basePrice, Integer maxCapacity, int enrolledStudents,
        int availableSeats, SpecialtyResponse specialty, CourseInstructor instructor) {

    public record CourseInstructor(Long id, String name, String lastName, String email) {
    }
}
