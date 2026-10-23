package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un administrador
@Getter
@Setter
public class AdministratorRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
