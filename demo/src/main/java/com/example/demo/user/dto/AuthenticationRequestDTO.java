package com.example.demo.user.dto;

// Credenciales de login compartidas por instructor, alumno y administrador.
public record AuthenticationRequestDTO(String email, String password) {
}
