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
    private static final Duration EXPIRATION = Duration.ofDays(10);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenUtil(String secret) {
        Objects.requireNonNull(secret, "La clave del token no puede ser nula");
        this.algorithm = Algorithm.HMAC512(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(String subject) {
        Objects.requireNonNull(subject, "El subject no puede ser nulo");
        Instant now = Instant.now();
        return PREFIX + JWT.create()
                .withSubject(subject)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(EXPIRATION))
                .sign(algorithm);
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

    private String withoutPrefix(String token) {
        return token.startsWith(PREFIX) ? token.substring(PREFIX.length()) : token;
    }
}
