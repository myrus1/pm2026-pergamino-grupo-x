package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Salida publica del instructor: sin password (las especialidades se gestionan por separado)
@AllArgsConstructor
@Getter
public class InstructorResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
