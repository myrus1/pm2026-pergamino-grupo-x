package com.example.demo.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.auth.model.AuthorizationException;
import org.junit.jupiter.api.Test;

class UsuarioAutenticadoTest {

    private static final String MENSAJE = "La inscripcion no le pertenece";

    private UsuarioAutenticado administrador() {
        return new UsuarioAutenticado(3L, "ada@example.com", Rol.ADMINISTRADOR);
    }

    private UsuarioAutenticado instructor() {
        return new UsuarioAutenticado(4L, "iris@example.com", Rol.INSTRUCTOR);
    }

    private UsuarioAutenticado alumno() {
        return new UsuarioAutenticado(9L, "ana@example.com", Rol.ALUMNO);
    }

    @Test
    void exigirPropioPermiteAlAdministradorDeAjenos() {
        assertThatCode(() -> administrador().exigirPropio(99L, MENSAJE)).doesNotThrowAnyException();
    }

    @Test
    void exigirPropioPermiteAlDuenoDelRecurso() {
        assertThatCode(() -> instructor().exigirPropio(4L, MENSAJE)).doesNotThrowAnyException();
        assertThatCode(() -> alumno().exigirPropio(9L, MENSAJE)).doesNotThrowAnyException();
    }

    @Test
    void exigirPropioLanzaExcepcionCuandoElRecursoEsDeOtro() {
        assertThatThrownBy(() -> instructor().exigirPropio(5L, MENSAJE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(MENSAJE);
        assertThatThrownBy(() -> alumno().exigirPropio(7L, MENSAJE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(MENSAJE);
    }

    @Test
    void exigirPropioLanzaExcepcionCuandoElIdEsNulo() {
        assertThatThrownBy(() -> instructor().exigirPropio(null, MENSAJE))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage(MENSAJE);
    }
}
