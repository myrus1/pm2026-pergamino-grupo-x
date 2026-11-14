package ar.edu.unnoba.pm2026.shared.security;

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
    private static final String CLAIM_ROLE = "rol";
    private static final String CLAIM_USER_ID = "userId";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenUtil(String secret) {
        Objects.requireNonNull(secret, "La clave del token no puede ser nula");
        this.algorithm = Algorithm.HMAC512(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(String subject, Role role, Long userId) {
        Objects.requireNonNull(subject, "El subject no puede ser nulo");
        Objects.requireNonNull(role, "El rol no puede ser nulo");
        Objects.requireNonNull(userId, "El id del usuario no puede ser nulo");
        Instant now = Instant.now();
        return PREFIX + JWT.create()
                .withSubject(subject)
                .withClaim(CLAIM_ROLE, role.name())
                .withClaim(CLAIM_USER_ID, userId)
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
        } catch (JWTVerificationException exception) {
            return false;
        }
    }

    public String getSubject(String token) {
        return JWT.decode(withoutPrefix(token)).getSubject();
    }

    public Role getRole(String token) {
        String role = JWT.decode(withoutPrefix(token)).getClaim(CLAIM_ROLE).asString();
        return role == null ? null : Role.valueOf(role);
    }

    public Long getUserId(String token) {
        return JWT.decode(withoutPrefix(token)).getClaim(CLAIM_USER_ID).asLong();
    }

    private String withoutPrefix(String token) {
        return token.startsWith(PREFIX) ? token.substring(PREFIX.length()) : token;
    }
}
