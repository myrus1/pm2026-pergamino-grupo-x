package com.example.demo.course.dto;

import com.example.demo.course.model.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un curso.
// status es opcional: si no viene, el curso conserva su estado actual.
@Getter
@Setter
public class CourseRequest {

    private String title;
    private String description;
    private BigDecimal basePrice;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxCapacity;
    private CourseStatus status;
    private Long specialtyId;
    private Long instructorId;
}
