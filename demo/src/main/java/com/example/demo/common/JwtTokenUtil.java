package com.example.demo.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class JwtTokenUtil {

    private static final String PREFIX = "Bearer ";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_USER_ID = "userId";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration expiration;

    public JwtTokenUtil(String secret, Duration expiration) {
        Objects.requireNonNull(secret, "La clave del token no puede ser nula");
        Objects.requireNonNull(expiration, "La duracion del token no puede ser nula");
        this.algorithm = Algorithm.HMAC512(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expiration = expiration;
    }

    // Devuelve el JWT sin el prefijo "Bearer ": el tipo de token viaja aparte en las
    // respuestas nuevas y los logins viejos lo anteponen para conservar su contrato.
    public String generateToken(String subject, Rol rol, Long userId) {
        Objects.requireNonNull(subject, "El subject no puede ser nulo");
        Objects.requireNonNull(rol, "El rol no puede ser nulo");
        Objects.requireNonNull(userId, "El id del usuario no puede ser nulo");
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(subject)
                .withClaim(CLAIM_ROL, rol.name())
                .withClaim(CLAIM_USER_ID, userId)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(expiration))
                .sign(algorithm);
    }

    public long getExpirationSeconds() {
        return expiration.getSeconds();
    }

    public boolean verify(String token) {
        if (token == null) {
            return false;
        }
        try {
            verifier.verify(withoutPrefix(token));
            return true;
        } catch (JWTVerificationException excepcion) {
            return false;
        }
    }

    public String getSubject(String token) {
        return JWT.decode(withoutPrefix(token)).getSubject();
    }

    public Rol getRol(String token) {
        String rol = JWT.decode(withoutPrefix(token)).getClaim(CLAIM_ROL).asString();
        if (rol == null) {
            return null;
        }
        try {
            return Rol.valueOf(rol);
        } catch (IllegalArgumentException excepcion) {
            // Tokens emitidos antes del rename de los roles (ALUMNO/ADMINISTRADOR):
            // se tratan como token sin rol valido, no como error interno.
            return null;
        }
    }

    public Long getUserId(String token) {
        return JWT.decode(withoutPrefix(token)).getClaim(CLAIM_USER_ID).asLong();
    }

    private String withoutPrefix(String token) {
        return token.startsWith(PREFIX) ? token.substring(PREFIX.length()) : token;
    }
}
