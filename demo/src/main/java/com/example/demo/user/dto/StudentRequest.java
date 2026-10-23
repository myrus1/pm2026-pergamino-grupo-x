package com.example.demo.user.dto;

import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un alumno (el password nunca se devuelve)
@Getter
@Setter
public class StudentRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String dni;
}
