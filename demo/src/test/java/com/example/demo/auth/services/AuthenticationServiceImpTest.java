package com.example.demo.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.auth.dto.AuthenticationResult;
import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;
import com.example.demo.common.Rol;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import com.example.demo.user.repositories.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImpTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba", Duration.ofSeconds(864000));

    private AuthenticationServiceImp authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImp(userRepository, passwordEncoder, jwtTokenUtil);
    }

    private Student alumnoRegistrado() {
        Student alumno = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        TestIds.fijarId(alumno, 9L);
        return alumno;
    }

    private Instructor instructorRegistrado() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com",
                passwordEncoder.encode("secret"));
        TestIds.fijarId(instructor, 4L);
        return instructor;
    }

    private Administrator administradorRegistrado() {
        Administrator administrador = new Administrator("Ada", "Lopez", "ada@example.com",
                passwordEncoder.encode("secret"));
        TestIds.fijarId(administrador, 3L);
        return administrador;
    }

    @Test
    void authenticateDeAlumnoDevuelveUnTokenCrudoConRolStudent() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(alumnoRegistrado()));

        AuthenticationResult resultado = authenticationService.authenticate("ana@example.com", "secret");

        assertThat(resultado.token()).doesNotStartWith("Bearer ");
        assertThat(jwtTokenUtil.verify(resultado.token())).isTrue();
        assertThat(jwtTokenUtil.getSubject(resultado.token())).isEqualTo("ana@example.com");
        assertThat(jwtTokenUtil.getRol(resultado.token())).isEqualTo(Rol.STUDENT);
        assertThat(jwtTokenUtil.getUserId(resultado.token())).isEqualTo(9L);
        assertThat(resultado.rol()).isEqualTo(Rol.STUDENT);
        assertThat(resultado.expiresIn()).isEqualTo(864000);
    }

    @Test
    void authenticateDeInstructorDevuelveUnTokenConRolInstructor() {
        when(userRepository.findByEmail("iris@example.com")).thenReturn(Optional.of(instructorRegistrado()));

        AuthenticationResult resultado = authenticationService.authenticate("iris@example.com", "secret");

        assertThat(jwtTokenUtil.getRol(resultado.token())).isEqualTo(Rol.INSTRUCTOR);
        assertThat(jwtTokenUtil.getUserId(resultado.token())).isEqualTo(4L);
        assertThat(resultado.rol()).isEqualTo(Rol.INSTRUCTOR);
    }

    @Test
    void authenticateDeAdministradorDevuelveUnTokenConRolAdministrator() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(administradorRegistrado()));

        AuthenticationResult resultado = authenticationService.authenticate("ada@example.com", "secret");

        assertThat(jwtTokenUtil.getRol(resultado.token())).isEqualTo(Rol.ADMINISTRATOR);
        assertThat(jwtTokenUtil.getUserId(resultado.token())).isEqualTo(3L);
        assertThat(resultado.rol()).isEqualTo(Rol.ADMINISTRATOR);
    }

    @Test
    void authenticateConEmailInexistenteLanzaExcepcion() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate("nadie@example.com", "secret"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateConContrasenaIncorrectaLanzaExcepcion() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(alumnoRegistrado()));

        assertThatThrownBy(() -> authenticationService.authenticate("ana@example.com", "otra"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateConContrasenaGuardadaSinHashLanzaExcepcionDeCredenciales() {
        Instructor viejo = new Instructor("Viejo", "Usuario", "viejo@example.com", "clave123");
        when(userRepository.findByEmail("viejo@example.com")).thenReturn(Optional.of(viejo));

        assertThatThrownBy(() -> authenticationService.authenticate("viejo@example.com", "clave123"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void elLoginViejoDeInstructorSigueDevolviendoElTokenConPrefijoBearer() {
        when(userRepository.findByEmail("iris@example.com")).thenReturn(Optional.of(instructorRegistrado()));

        String token = authenticationService.authenticate(
                new Instructor(null, null, "iris@example.com", "secret"));

        assertThat(token).startsWith("Bearer ");
        assertThat(jwtTokenUtil.getRol(token)).isEqualTo(Rol.INSTRUCTOR);
    }

    @Test
    void elLoginViejoDeAlumnoRechazaAUnInstructor() {
        when(userRepository.findByEmail("iris@example.com")).thenReturn(Optional.of(instructorRegistrado()));

        assertThatThrownBy(() -> authenticationService.authenticate(
                new Student(null, null, "iris@example.com", "secret", null)))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }
}
