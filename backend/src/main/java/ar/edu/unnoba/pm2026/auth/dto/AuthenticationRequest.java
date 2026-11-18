package ar.edu.unnoba.pm2026.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Credenciales de login compartidas por instructor, alumno y administrador.
public record AuthenticationRequest(
        @NotBlank @Email(message = "El campo 'email' debe tener un formato valido") String email,
        @NotBlank String password) {
}
