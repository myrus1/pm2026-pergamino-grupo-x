package com.example.demo.user.dto;

import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un instructor
@Getter
@Setter
public class InstructorRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
