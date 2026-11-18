package ar.edu.unnoba.pm2026.course.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un curso.
// El estado no se recibe: solo cambia con las solicitudes de aprobacion.
@Getter
@Setter
public class CourseRequest {

    @NotBlank
    private String title;
    private String description;
    @NotNull
    @DecimalMin(value = "0.0", message = "El precio base no puede ser negativo")
    private BigDecimal basePrice;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotNull
    @Positive(message = "La capacidad debe ser mayor a cero")
    private Integer maxCapacity;
    @NotNull
    private Long specialtyId;
    private Long instructorId;
}
