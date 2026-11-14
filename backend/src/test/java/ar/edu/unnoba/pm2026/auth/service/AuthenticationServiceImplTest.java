package ar.edu.unnoba.pm2026.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private InstructorService instructorService;

    @Mock
    private StudentService studentService;

    @Mock
    private AdministratorService administratorService;

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(instructorService, studentService,
                administratorService, passwordEncoder, jwtTokenUtil);
    }

    private Instructor credentials(String email, String password) {
        return new Instructor(null, null, email, password);
    }

    @Test
    void authenticateConCredencialesValidasDevuelveUnTokenDelInstructor() throws Exception {
        Instructor registeredUser = new Instructor("Iris", "Ruiz", "iris@example.com",
                passwordEncoder.encode("secret"));
        TestIds.setId(registeredUser, 4L);
        when(instructorService.findByEmail("iris@example.com")).thenReturn(registeredUser);

        String token = authenticationService.authenticate(credentials("iris@example.com", "secret"));

        assertThat(token).startsWith("Bearer ");
        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getSubject(token)).isEqualTo("iris@example.com");
        assertThat(jwtTokenUtil.getRole(token)).isEqualTo(Role.INSTRUCTOR);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(4L);
    }

    @Test
    void authenticateDeAlumnoDevuelveUnTokenConRolAlumno() throws Exception {
        Student registeredUser = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        TestIds.setId(registeredUser, 9L);
        when(studentService.findByEmail("ana@example.com")).thenReturn(registeredUser);

        String token = authenticationService.authenticate(new Student(null, null, "ana@example.com", "secret", null));

        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getRole(token)).isEqualTo(Role.STUDENT);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(9L);
    }

    @Test
    void authenticateDeAdministradorDevuelveUnTokenConRolAdministrador() throws Exception {
        Administrator registeredUser = new Administrator("Ada", "Lopez", "ada@example.com",
                passwordEncoder.encode("secret"));
        TestIds.setId(registeredUser, 3L);
        when(administratorService.findByEmail("ada@example.com")).thenReturn(registeredUser);

        String token = authenticationService.authenticate(new Administrator(null, null, "ada@example.com", "secret"));

        assertThat(jwtTokenUtil.verify(token)).isTrue();
        assertThat(jwtTokenUtil.getRole(token)).isEqualTo(Role.ADMINISTRATOR);
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(3L);
    }

    @Test
    void authenticateConEmailInexistenteLanzaExcepcion() {
        when(instructorService.findByEmail("nadie@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.authenticate(credentials("nadie@example.com", "secret")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateDeAlumnoConContrasenaIncorrectaLanzaExcepcion() {
        Student registeredUser = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        when(studentService.findByEmail("ana@example.com")).thenReturn(registeredUser);

        assertThatThrownBy(() -> authenticationService
                .authenticate(new Student(null, null, "ana@example.com", "otra", null)))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateDeAdministradorConContrasenaIncorrectaLanzaExcepcion() {
        Administrator registeredUser = new Administrator("Ada", "Lopez", "ada@example.com",
                passwordEncoder.encode("secret"));
        when(administratorService.findByEmail("ada@example.com")).thenReturn(registeredUser);

        assertThatThrownBy(() -> authenticationService
                .authenticate(new Administrator(null, null, "ada@example.com", "otra")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateConContrasenaIncorrectaLanzaExcepcion() {
        when(instructorService.findByEmail("iris@example.com"))
                .thenReturn(new Instructor("Iris", "Ruiz", "iris@example.com", passwordEncoder.encode("secret")));

        assertThatThrownBy(() -> authenticationService.authenticate(credentials("iris@example.com", "otra")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateConContrasenaGuardadaSinHashLanzaExcepcionDeCredenciales() {
        when(instructorService.findByEmail("viejo@example.com"))
                .thenReturn(new Instructor("Viejo", "Usuario", "viejo@example.com", "clave123"));

        assertThatThrownBy(() -> authenticationService.authenticate(credentials("viejo@example.com", "clave123")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }
}
