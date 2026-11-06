package com.example.demo.course.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un curso.
// El estado no se recibe: solo cambia con las solicitudes de aprobacion.
@Getter
@Setter
public class CourseRequest {

    private String title;
    private String description;
    private BigDecimal basePrice;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxCapacity;
    private Long specialtyId;
    private Long instructorId;
}
