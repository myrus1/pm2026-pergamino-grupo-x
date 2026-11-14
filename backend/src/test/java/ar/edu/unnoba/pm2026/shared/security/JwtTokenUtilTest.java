package ar.edu.unnoba.pm2026.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtTokenUtilTest {

    private static final String CLAVE = "clave-de-prueba-para-los-tests";

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(CLAVE);

    @Test
    void generateTokenAnteponeBearerYGuardaElEmailComoSubject() {
        String token = jwtTokenUtil.generateToken("iris@example.com", Role.INSTRUCTOR, 4L);

        assertThat(token).startsWith("Bearer ");
        assertThat(jwtTokenUtil.getSubject(token)).isEqualTo("iris@example.com");
    }

    @Test
    void generateTokenGuardaElRolYElIdDelUsuarioComoClaims() {
        String token = jwtTokenUtil.generateToken("iris@example.com", Role.STUDENT, 9L);

        assertThat(jwtTokenUtil.getRole(token)).isEqualTo(Role.STUDENT);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(9L);
    }

    @Test
    void generateTokenNoSePuedeFirmarSinRolNiIdDeUsuario() {
        assertThatThrownBy(() -> jwtTokenUtil.generateToken("iris@example.com", null, 4L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> jwtTokenUtil.generateToken("iris@example.com", Role.STUDENT, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getRolDevuelveNullCuandoElTokenNoLlevaElClaim() {
        String withoutRole = JWT.create().withSubject("iris@example.com").sign(Algorithm.HMAC512(CLAVE));

        assertThat(jwtTokenUtil.getRole(withoutRole)).isNull();
    }

    @Test
    void elTokenVenceEnDiezDiasYUsaHmac512() {
        DecodedJWT decoded = JWT.decode(
                jwtTokenUtil.generateToken("iris@example.com", Role.INSTRUCTOR, 4L).substring(7));

        assertThat(Duration.between(decoded.getIssuedAtAsInstant(), decoded.getExpiresAtAsInstant()))
                .isEqualTo(Duration.ofDays(10));
        assertThat(decoded.getAlgorithm()).isEqualTo("HS512");
    }

    @Test
    void verifyAceptaElTokenConOSinPrefijo() {
        String token = jwtTokenUtil.generateToken("iris@example.com", Role.INSTRUCTOR, 4L);

        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.verify(token.substring(7))).isTrue();
    }

    @Test
    void verifyRechazaUnTokenFirmadoConOtraClave() {
        String foreign = new JwtTokenUtil("otra-clave").generateToken("iris@example.com", Role.INSTRUCTOR, 4L);

        assertThat(jwtTokenUtil.verify(foreign)).isFalse();
    }

    @Test
    void verifyRechazaUnTokenVencido() {
        String expired = JWT.create().withSubject("iris@example.com")
                .withExpiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .sign(Algorithm.HMAC512(CLAVE));

        assertThat(jwtTokenUtil.verify(expired)).isFalse();
    }

    @Test
    void verifyRechazaTextoQueNoEsUnTokenYNulo() {
        assertThat(jwtTokenUtil.verify("Bearer cualquier-cosa")).isFalse();
        assertThat(jwtTokenUtil.verify(null)).isFalse();
    }
}
