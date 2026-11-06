package com.example.demo.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImpTest {

    @Mock
    private InstructorService instructorService;

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");

    private AuthenticationServiceImp authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImp(instructorService, passwordEncoder, jwtTokenUtil);
    }

    private Instructor credenciales(String email, String password) {
        return new Instructor(null, null, email, password);
    }

    @Test
    void authenticateConCredencialesValidasDevuelveUnTokenDelInstructor() throws Exception {
        when(instructorService.findByEmail("iris@example.com"))
                .thenReturn(new Instructor("Iris", "Ruiz", "iris@example.com", passwordEncoder.encode("secret")));

        String token = authenticationService.authenticate(credenciales("iris@example.com", "secret"));

        assertThat(token).startsWith("Bearer ");
        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getSubject(token)).isEqualTo("iris@example.com");
    }

    @Test
    void authenticateConEmailInexistenteLanzaExcepcion() {
        when(instructorService.findByEmail("nadie@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.authenticate(credenciales("nadie@example.com", "secret")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateConContrasenaIncorrectaLanzaExcepcion() {
        when(instructorService.findByEmail("iris@example.com"))
                .thenReturn(new Instructor("Iris", "Ruiz", "iris@example.com", passwordEncoder.encode("secret")));

        assertThatThrownBy(() -> authenticationService.authenticate(credenciales("iris@example.com", "otra")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateConContrasenaGuardadaSinHashLanzaExcepcionDeCredenciales() {
        when(instructorService.findByEmail("viejo@example.com"))
                .thenReturn(new Instructor("Viejo", "Usuario", "viejo@example.com", "clave123"));

        assertThatThrownBy(() -> authenticationService.authenticate(credenciales("viejo@example.com", "clave123")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }
}
