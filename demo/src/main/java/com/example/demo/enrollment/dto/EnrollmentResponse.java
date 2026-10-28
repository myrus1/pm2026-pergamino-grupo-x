package com.example.demo.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Salida publica de una inscripcion: el alumno y el curso se referencian por id
@AllArgsConstructor
@Getter
public class EnrollmentResponse {

    private Long id;
    private LocalDateTime enrollmentDate;
    private BigDecimal finalAmount;
    private Long studentId;
    private Long courseId;
}
