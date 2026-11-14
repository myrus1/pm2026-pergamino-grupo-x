package ar.edu.unnoba.pm2026.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

    private static final String MESSAGE = "La inscripcion no le pertenece";

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
        assertThatCode(() -> administrator().requireOwnership(99L, MESSAGE)).doesNotThrowAnyException();
    }

    @Test
    void exigirPropioPermiteAlDuenoDelRecurso() {
        assertThatCode(() -> instructor().requireOwnership(4L, MESSAGE)).doesNotThrowAnyException();
        assertThatCode(() -> student().requireOwnership(9L, MESSAGE)).doesNotThrowAnyException();
    }

    @Test
    void exigirPropioLanzaExcepcionCuandoElRecursoEsDeOtro() {
        assertThatThrownBy(() -> instructor().requireOwnership(5L, MESSAGE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(MESSAGE);
        assertThatThrownBy(() -> student().requireOwnership(7L, MESSAGE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(MESSAGE);
    }

    @Test
    void exigirPropioLanzaExcepcionCuandoElIdEsNulo() {
        assertThatThrownBy(() -> instructor().requireOwnership(null, MESSAGE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(MESSAGE);
    }
}
