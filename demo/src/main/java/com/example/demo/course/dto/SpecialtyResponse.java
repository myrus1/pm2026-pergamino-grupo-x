package com.example.demo.course.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Salida publica de una especialidad
@AllArgsConstructor
@Getter
public class SpecialtyResponse {

    private Long id;
    private String name;
}
