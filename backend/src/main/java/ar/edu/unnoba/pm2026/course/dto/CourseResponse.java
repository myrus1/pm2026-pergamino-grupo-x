package ar.edu.unnoba.pm2026.course.dto;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Salida publica del curso: las relaciones se referencian por id
@AllArgsConstructor
@Getter
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxCapacity;
    private CourseStatus status;
    private Long specialtyId;
    private Long instructorId;

    // Solo lee los ids de las relaciones, asi que se puede armar fuera de una transaccion
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(), course.getBasePrice(),
                course.getStartDate(), course.getEndDate(), course.getMaxCapacity(), course.getStatus(),
                course.getSpecialty() == null ? null : course.getSpecialty().getId(),
                course.getInstructor() == null ? null : course.getInstructor().getId());
    }
}
