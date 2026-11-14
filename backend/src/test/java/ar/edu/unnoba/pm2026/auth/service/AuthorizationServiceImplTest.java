package ar.edu.unnoba.pm2026.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
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
class AuthorizationServiceImplTest {

    @Mock
    private InstructorService instructorService;

    @Mock
    private StudentService studentService;

    @Mock
    private AdministratorService administratorService;

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");

    private AuthorizationServiceImpl authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImpl(jwtTokenUtil, instructorService, studentService,
                administratorService);
    }

    @Test
    void autorizarConRolPermitidoDevuelveElUsuarioDelToken() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "hash");
        TestIds.setId(instructor, 4L);
        when(instructorService.findByEmail("iris@example.com")).thenReturn(instructor);
        String token = jwtTokenUtil.generateToken("iris@example.com", Role.INSTRUCTOR, 4L);

        AuthenticatedUser authenticatedUser = authorizationService.authorize(token, Role.INSTRUCTOR);

        assertThat(authenticatedUser.id()).isEqualTo(4L);
        assertThat(authenticatedUser.email()).isEqualTo("iris@example.com");
        assertThat(authenticatedUser.role()).isEqualTo(Role.INSTRUCTOR);
    }

    @Test
    void autorizarAceptaCualquieraDeLosRolesPermitidos() {
        Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "hash");
        TestIds.setId(admin, 3L);
        when(administratorService.findByEmail("ada@example.com")).thenReturn(admin);
        String token = jwtTokenUtil.generateToken("ada@example.com", Role.ADMINISTRATOR, 3L);

        assertThat(authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR).role())
                .isEqualTo(Role.ADMINISTRATOR);
    }

    @Test
    void autorizarConRolNoPermitidoLanzaExcepcionDeAutorizacion() {
        String token = jwtTokenUtil.generateToken("ana@example.com", Role.STUDENT, 9L);

        assertThatThrownBy(() -> authorizationService.authorize(token, Role.ADMINISTRATOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("No tiene permisos para esta operacion");
    }

    @Test
    void autorizarSinHeaderAuthorizationLanzaExcepcionDeAutenticacion() {
        assertThatThrownBy(() -> authorizationService.authorize(null, Role.INSTRUCTOR))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConTokenInvalidoLanzaExcepcionDeAutenticacion() {
        assertThatThrownBy(() -> authorizationService.authorize("Bearer no-es-un-token", Role.INSTRUCTOR))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConTokenDeOtraClaveLanzaExcepcionDeAutenticacion() {
        String foreign = new JwtTokenUtil("otra-clave").generateToken("iris@example.com", Role.INSTRUCTOR, 4L);

        assertThatThrownBy(() -> authorizationService.authorize(foreign, Role.INSTRUCTOR))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConTokenSinRolLanzaExcepcionDeAutorizacion() {
        String withoutRole = com.auth0.jwt.JWT.create().withSubject("iris@example.com")
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC512("clave-de-prueba"));

        assertThatThrownBy(() -> authorizationService.authorize(withoutRole, Role.INSTRUCTOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El token no tiene rol asignado");
    }

    // Tokens emitidos antes del cambio Rol -> Role traen "ALUMNO" o "ADMINISTRADOR":
    // tienen que pedir login de nuevo (401), no romper con un 400
    @Test
    void autorizarConRolDesconocidoEnElTokenLanzaExcepcionDeAutenticacion() {
        String oldRole = com.auth0.jwt.JWT.create().withSubject("ana@example.com").withClaim("rol", "ALUMNO")
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC512("clave-de-prueba"));

        assertThatThrownBy(() -> authorizationService.authorize(oldRole, Role.STUDENT))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConUsuarioQueYaNoExisteLanzaExcepcionDeAutorizacion() {
        when(instructorService.findByEmail("borrado@example.com")).thenReturn(null);
        String token = jwtTokenUtil.generateToken("borrado@example.com", Role.INSTRUCTOR, 4L);

        assertThatThrownBy(() -> authorizationService.authorize(token, Role.INSTRUCTOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El usuario del token ya no existe");
    }

    @Test
    void autorizarComoAlumnoBuscaElAlumno() {
        Student student = new Student("Ana", "Perez", "ana@example.com", "hash", "11111111");
        TestIds.setId(student, 9L);
        when(studentService.findByEmail("ana@example.com")).thenReturn(student);
        String token = jwtTokenUtil.generateToken("ana@example.com", Role.STUDENT, 9L);

        assertThat(authorizationService.authorize(token, Role.STUDENT).id()).isEqualTo(9L);
    }
}
