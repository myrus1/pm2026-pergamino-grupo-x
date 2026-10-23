package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Salida publica del administrador: sin password
@AllArgsConstructor
@Getter
public class AdministratorResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
