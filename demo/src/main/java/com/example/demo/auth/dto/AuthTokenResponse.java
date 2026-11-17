package com.example.demo.auth.dto;

import com.example.demo.common.Rol;

// Respuesta del login unico: POST /api/auth/token
public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn, Rol role) {

    public static final String TOKEN_TYPE = "Bearer";

    public static AuthTokenResponse desde(AuthenticationResult resultado) {
        return new AuthTokenResponse(resultado.token(), TOKEN_TYPE, resultado.expiresIn(), resultado.rol());
    }
}
