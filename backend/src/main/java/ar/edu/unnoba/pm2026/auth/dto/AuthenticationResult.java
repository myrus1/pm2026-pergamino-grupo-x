package ar.edu.unnoba.pm2026.auth.dto;

import ar.edu.unnoba.pm2026.shared.security.Role;

// Resultado interno del login unico: JWT sin el prefijo "Bearer ", rol y duracion en segundos.
public record AuthenticationResult(String token, Role role, long expiresIn) {
}
