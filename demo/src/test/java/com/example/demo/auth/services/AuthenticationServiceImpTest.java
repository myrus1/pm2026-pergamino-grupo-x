package com.example.demo.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;
import com.example.demo.common.Rol;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import com.example.demo.user.services.AdministratorService;
import com.example.demo.user.services.InstructorService;
import com.example.demo.user.services.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImpTest {

    @Mock
    private InstructorService instructorService;

    @Mock
    private StudentService studentService;

    @Mock
    private AdministratorService administratorService;

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");

    private AuthenticationServiceImp authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImp(instructorService, studentService,
                administratorService, passwordEncoder, jwtTokenUtil);
    }

    private Instructor credenciales(String email, String password) {
        return new Instructor(null, null, email, password);
    }

    @Test
    void authenticateConCredencialesValidasDevuelveUnTokenDelInstructor() throws Exception {
        Instructor registrado = new Instructor("Iris", "Ruiz", "iris@example.com",
                passwordEncoder.encode("secret"));
        TestIds.fijarId(registrado, 4L);
        when(instructorService.findByEmail("iris@example.com")).thenReturn(registrado);

        String token = authenticationService.authenticate(credenciales("iris@example.com", "secret"));

        assertThat(token).startsWith("Bearer ");
        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getSubject(token)).isEqualTo("iris@example.com");
        assertThat(jwtTokenUtil.getRol(token)).isEqualTo(Rol.INSTRUCTOR);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(4L);
    }

    @Test
    void authenticateDeAlumnoDevuelveUnTokenConRolAlumno() throws Exception {
        Student registrado = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        TestIds.fijarId(registrado, 9L);
        when(studentService.findByEmail("ana@example.com")).thenReturn(registrado);

        String token = authenticationService.authenticate(new Student(null, null, "ana@example.com", "secret", null));

        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getRol(token)).isEqualTo(Rol.ALUMNO);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(9L);
    }

    @Test
    void authenticateDeAdministradorDevuelveUnTokenConRolAdministrador() throws Exception {
        Administrator registrado = new Administrator("Ada", "Lopez", "ada@example.com",
                passwordEncoder.encode("secret"));
        TestIds.fijarId(registrado, 3L);
        when(administratorService.findByEmail("ada@example.com")).thenReturn(registrado);

        String token = authenticationService.authenticate(new Administrator(null, null, "ada@example.com", "secret"));

        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getRol(token)).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(3L);
    }

    @Test
    void authenticateConEmailInexistenteLanzaExcepcion() {
        when(instructorService.findByEmail("nadie@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.authenticate(credenciales("nadie@example.com", "secret")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateDeAlumnoConContrasenaIncorrectaLanzaExcepcion() {
        Student registrado = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        when(studentService.findByEmail("ana@example.com")).thenReturn(registrado);

        assertThatThrownBy(() -> authenticationService
                .authenticate(new Student(null, null, "ana@example.com", "otra", null)))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateDeAdministradorConContrasenaIncorrectaLanzaExcepcion() {
        Administrator registrado = new Administrator("Ada", "Lopez", "ada@example.com",
                passwordEncoder.encode("secret"));
        when(administratorService.findByEmail("ada@example.com")).thenReturn(registrado);

        assertThatThrownBy(() -> authenticationService
                .authenticate(new Administrator(null, null, "ada@example.com", "otra")))
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
