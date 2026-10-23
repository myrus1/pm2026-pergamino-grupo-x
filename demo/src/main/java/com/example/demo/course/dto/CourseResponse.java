package com.example.demo.course.dto;

import com.example.demo.course.model.CourseStatus;
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
}
