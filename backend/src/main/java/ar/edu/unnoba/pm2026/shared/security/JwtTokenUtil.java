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
    private static final Duration DEFAULT_EXPIRATION = Duration.ofDays(10);
    private static final String CLAIM_ROLE = "rol";
    private static final String CLAIM_USER_ID = "userId";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration expiration;

    public JwtTokenUtil(String secret) {
        this(secret, DEFAULT_EXPIRATION);
    }

    public JwtTokenUtil(String secret, Duration expiration) {
        Objects.requireNonNull(secret, "JWT secret cannot be null");
        Objects.requireNonNull(expiration, "JWT expiration cannot be null");
        if (expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.algorithm = Algorithm.HMAC512(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expiration = expiration;
    }

    // Token con el prefijo "Bearer ": es lo que devuelven los logins por rol ({"token": "Bearer ..."}).
    public String generateToken(String subject, Role role, Long userId) {
        return PREFIX + generateRawToken(subject, role, userId);
    }

    // JWT sin el prefijo: el login unico lo devuelve en accessToken y manda el tipo aparte (tokenType).
    public String generateRawToken(String subject, Role role, Long userId) {
        Objects.requireNonNull(subject, "JWT subject cannot be null");
        Objects.requireNonNull(role, "JWT role cannot be null");
        Objects.requireNonNull(userId, "JWT user id cannot be null");
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(subject)
                .withClaim(CLAIM_ROLE, role.claimValue())
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
        } catch (JWTVerificationException exception) {
            return false;
        }
    }

    public String getSubject(String token) {
        return JWT.decode(withoutPrefix(token)).getSubject();
    }

    public Role getRole(String token) {
        String role = JWT.decode(withoutPrefix(token)).getClaim(CLAIM_ROLE).asString();
        return role == null ? null : Role.fromClaimValue(role);
    }

    public Long getUserId(String token) {
        return JWT.decode(withoutPrefix(token)).getClaim(CLAIM_USER_ID).asLong();
    }

    private String withoutPrefix(String token) {
        return token.startsWith(PREFIX) ? token.substring(PREFIX.length()) : token;
    }
}
