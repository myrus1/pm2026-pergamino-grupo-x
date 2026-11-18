package ar.edu.unnoba.pm2026.enrollment.dto;

import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

// TP6: un curso visto por el alumno del token (catalogo y detalle). applicablePrice es lo que pagaria hoy
// (Student.priceFor): con el 25% de descuento de fidelidad si loyaltyDiscount es true.
public record StudentCourseResponse(Long id, String title, String description, LocalDate startDate,
        LocalDate endDate, Integer maxCapacity, int availableSeats, BigDecimal basePrice, BigDecimal applicablePrice,
        boolean loyaltyDiscount, boolean enrolled, SpecialtyResponse specialty,
        CourseDetailResponse.CourseInstructor instructor) {
}
