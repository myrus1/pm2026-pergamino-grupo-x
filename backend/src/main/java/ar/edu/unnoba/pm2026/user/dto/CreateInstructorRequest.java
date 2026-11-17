package ar.edu.unnoba.pm2026.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInstructorRequest(
        @NotBlank @Email(message = "El campo 'email' debe tener un formato valido") String email,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName) {
}
