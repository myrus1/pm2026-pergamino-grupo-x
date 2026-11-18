package ar.edu.unnoba.pm2026.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar un instructor
@Getter
@Setter
public class InstructorRequest {

    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    @Email(message = "El campo 'email' debe tener un formato valido")
    private String email;
    @NotBlank
    private String password;
}
