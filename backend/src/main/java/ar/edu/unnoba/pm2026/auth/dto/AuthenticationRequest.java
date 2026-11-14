package ar.edu.unnoba.pm2026.auth.dto;

// Credenciales de login compartidas por instructor, alumno y administrador.
public record AuthenticationRequest(String email, String password) {
}
