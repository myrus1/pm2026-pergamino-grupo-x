package ar.edu.unnoba.pm2026.auth.dto;

import ar.edu.unnoba.pm2026.shared.security.Role;

// Respuesta de POST /api/auth/token. role va en ingles (STUDENT, INSTRUCTOR, ADMINISTRATOR);
// el claim "rol" dentro del token sigue con los valores que lee el front (ALUMNO, INSTRUCTOR, ADMINISTRADOR).
public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn, Role role) {

    public static final String TOKEN_TYPE = "Bearer";

    public static AuthTokenResponse from(AuthenticationResult result) {
        return new AuthTokenResponse(result.token(), TOKEN_TYPE, result.expiresIn(), result.role());
    }
}
