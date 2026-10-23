package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Salida publica del alumno: sin password
@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String dni;
}
