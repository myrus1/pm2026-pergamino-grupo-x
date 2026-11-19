package ar.edu.unnoba.pm2026.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un alumno (el password nunca se devuelve)
@Getter
@Setter
public class StudentRequest {

    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String dni;
}
