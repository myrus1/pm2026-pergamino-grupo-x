package ar.edu.unnoba.pm2026.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

    private static final ErrorCode CODE = ErrorCode.AUTHORIZATION_ENROLLMENT_NOT_OWNER;

    private AuthenticatedUser administrator() {
        return new AuthenticatedUser(3L, "ada@example.com", Role.ADMINISTRATOR);
    }

    private AuthenticatedUser instructor() {
        return new AuthenticatedUser(4L, "iris@example.com", Role.INSTRUCTOR);
    }

    private AuthenticatedUser student() {
        return new AuthenticatedUser(9L, "ana@example.com", Role.STUDENT);
    }

    @Test
    void exigirPropioPermiteAlAdministradorDeAjenos() {
        assertThatCode(() -> administrator().requireOwnership(99L, CODE)).doesNotThrowAnyException();
    }

    @Test
    void exigirPropioPermiteAlDuenoDelRecurso() {
        assertThatCode(() -> instructor().requireOwnership(4L, CODE)).doesNotThrowAnyException();
        assertThatCode(() -> student().requireOwnership(9L, CODE)).doesNotThrowAnyException();
    }

    @Test
    void exigirPropioLanzaExcepcionCuandoElRecursoEsDeOtro() {
        assertThatThrownBy(() -> instructor().requireOwnership(5L, CODE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(CODE.key());
        assertThatThrownBy(() -> student().requireOwnership(7L, CODE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(CODE.key());
    }

    @Test
    void exigirPropioLanzaExcepcionCuandoElIdEsNulo() {
        assertThatThrownBy(() -> instructor().requireOwnership(null, CODE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(CODE.key());
    }
}
