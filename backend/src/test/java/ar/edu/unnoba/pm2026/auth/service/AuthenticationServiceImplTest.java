package ar.edu.unnoba.pm2026.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.auth.dto.AuthenticationResult;
import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.repository.UserRepository;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import ar.edu.unnoba.pm2026.shared.observability.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Optional;
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

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(instructorService, studentService,
                administratorService, passwordEncoder, jwtTokenUtil, new BusinessMetrics(meterRegistry), userRepository);
    }

    private double logins(Role role, String result) {
        return meterRegistry.get("pm2026.auth.logins").tag("role", role.name()).tag("result", result).counter()
                .count();
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
        assertThat(logins(Role.INSTRUCTOR, "success")).isEqualTo(1);
        assertThat(logins(Role.INSTRUCTOR, "failure")).isZero();
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
        assertThat(logins(Role.STUDENT, "failure")).isEqualTo(1);
        assertThat(logins(Role.STUDENT, "success")).isZero();
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

    // Login unico (#27): POST /api/auth/token

    @Test
    void authenticateUnicoDevuelveElTokenSinPrefijoConElRolDelUsuario() throws Exception {
        Student registeredUser = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        TestIds.setId(registeredUser, 9L);
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(registeredUser));

        AuthenticationResult result = authenticationService.authenticate("ana@example.com", "secret");

        assertThat(result.role()).isEqualTo(Role.STUDENT);
        assertThat(result.expiresIn()).isEqualTo(864000);
        assertThat(result.token()).doesNotStartWith("Bearer ");
        assertThat(jwtTokenUtil.verify(result.token())).isTrue();
        assertThat(jwtTokenUtil.getSubject(result.token())).isEqualTo("ana@example.com");
        assertThat(jwtTokenUtil.getRole(result.token())).isEqualTo(Role.STUDENT);
        assertThat(jwtTokenUtil.getUserId(result.token())).isEqualTo(9L);
        assertThat(logins(Role.STUDENT, "success")).isEqualTo(1);
    }

    @Test
    void authenticateUnicoDetectaInstructoresYAdministradores() throws Exception {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", passwordEncoder.encode("secret"));
        TestIds.setId(instructor, 4L);
        Administrator administrator = new Administrator("Ada", "Lovelace", "ada@example.com",
                passwordEncoder.encode("secret"));
        TestIds.setId(administrator, 3L);
        when(userRepository.findByEmail("iris@example.com")).thenReturn(Optional.of(instructor));
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(administrator));

        assertThat(authenticationService.authenticate("iris@example.com", "secret").role())
                .isEqualTo(Role.INSTRUCTOR);
        assertThat(authenticationService.authenticate("ada@example.com", "secret").role())
                .isEqualTo(Role.ADMINISTRATOR);
        assertThat(logins(Role.INSTRUCTOR, "success")).isEqualTo(1);
        assertThat(logins(Role.ADMINISTRATOR, "success")).isEqualTo(1);
    }

    @Test
    void authenticateUnicoDevuelveLaDuracionConfigurada() throws Exception {
        JwtTokenUtil shortLived = new JwtTokenUtil("clave-de-prueba", Duration.ofSeconds(3600));
        AuthenticationServiceImpl service = new AuthenticationServiceImpl(instructorService, studentService,
                administratorService, passwordEncoder, shortLived, new BusinessMetrics(meterRegistry), userRepository);
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", passwordEncoder.encode("secret"));
        TestIds.setId(instructor, 4L);
        when(userRepository.findByEmail("iris@example.com")).thenReturn(Optional.of(instructor));

        assertThat(service.authenticate("iris@example.com", "secret").expiresIn()).isEqualTo(3600);
    }

    @Test
    void authenticateUnicoConPasswordIncorrectaLanzaAuthenticationException() throws Exception {
        Student registeredUser = new Student("Ana", "Perez", "ana@example.com",
                passwordEncoder.encode("secret"), "11111111");
        TestIds.setId(registeredUser, 9L);
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(registeredUser));

        assertThatThrownBy(() -> authenticationService.authenticate("ana@example.com", "otra"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
        assertThat(logins(Role.STUDENT, "failure")).isEqualTo(1);
        assertThat(logins(Role.STUDENT, "success")).isZero();
    }

    @Test
    void authenticateUnicoConEmailInexistenteLanzaAuthenticationException() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate("nadie@example.com", "secret"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void authenticateUnicoConHashQueNoEsBcryptLanzaAuthenticationException() throws Exception {
        Student registeredUser = new Student("Ana", "Perez", "ana@example.com", "no-es-un-hash", "11111111");
        TestIds.setId(registeredUser, 9L);
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(registeredUser));

        assertThatThrownBy(() -> authenticationService.authenticate("ana@example.com", "no-es-un-hash"))
                .isInstanceOf(AuthenticationException.class);
    }
}
