package ar.edu.unnoba.pm2026.course.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// TP6: PUT /instructor/courses/{id} (UpdateCourseRequestDTO: "JSON con campos a modificar").
// Todos los campos son opcionales; los que no vienen quedan como estaban. Course.applyChanges valida el
// resultado (p. ej. que la fecha de fin siga siendo posterior al inicio).
@Getter
@Setter
public class UpdateCourseRequest {

    @Pattern(regexp = ".*\\S.*")
    private String title;
    private String description;
    @DecimalMin(value = "0.0")
    private BigDecimal basePrice;
    private LocalDate startDate;
    private LocalDate endDate;
    @Positive
    private Integer maxCapacity;
    @JsonAlias("specialty")
    private Long specialtyId;
}
