package com.example.demo.auth.dto;

import com.example.demo.common.Rol;

// Resultado interno del login: token crudo (sin "Bearer "), rol y duracion configurada.
public record AuthenticationResult(String token, Rol rol, long expiresIn) {
}
