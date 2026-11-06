package com.example.demo.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImpTest {

    @Mock
    private InstructorService instructorService;

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");

    private AuthorizationServiceImp authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImp(jwtTokenUtil, instructorService);
    }

    @Test
    void authorizeConTokenValidoDevuelveElInstructorDelSubject() throws Exception {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "hash");
        when(instructorService.findByEmail("iris@example.com")).thenReturn(instructor);

        assertThat(authorizationService.authorize(jwtTokenUtil.generateToken("iris@example.com"))).isSameAs(instructor);
    }

    @Test
    void authorizeConTokenInvalidoLanzaExcepcion() {
        assertThatThrownBy(() -> authorizationService.authorize("Bearer no-es-un-token"))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void authorizeConTokenDeOtraClaveLanzaExcepcion() {
        String ajeno = new JwtTokenUtil("otra-clave").generateToken("iris@example.com");

        assertThatThrownBy(() -> authorizationService.authorize(ajeno))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void authorizeConEmailQueYaNoEsInstructorLanzaExcepcion() {
        when(instructorService.findByEmail("borrado@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authorizationService.authorize(jwtTokenUtil.generateToken("borrado@example.com")))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El token no corresponde a un instructor");
    }
}
