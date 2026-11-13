package com.example.demo.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.auth.model.UsuarioAutenticado;
import com.example.demo.common.JwtTokenUtil;
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
class AuthorizationServiceImpTest {

    @Mock
    private InstructorService instructorService;

    @Mock
    private StudentService studentService;

    @Mock
    private AdministratorService administratorService;

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil("clave-de-prueba");

    private AuthorizationServiceImp authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationServiceImp(jwtTokenUtil, instructorService, studentService,
                administratorService);
    }

    @Test
    void autorizarConRolPermitidoDevuelveElUsuarioDelToken() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "hash");
        TestIds.fijarId(instructor, 4L);
        when(instructorService.findByEmail("iris@example.com")).thenReturn(instructor);
        String token = jwtTokenUtil.generateToken("iris@example.com", Rol.INSTRUCTOR, 4L);

        UsuarioAutenticado usuario = authorizationService.autorizar(token, Rol.INSTRUCTOR);

        assertThat(usuario.id()).isEqualTo(4L);
        assertThat(usuario.email()).isEqualTo("iris@example.com");
        assertThat(usuario.rol()).isEqualTo(Rol.INSTRUCTOR);
    }

    @Test
    void autorizarAceptaCualquieraDeLosRolesPermitidos() {
        Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "hash");
        TestIds.fijarId(admin, 3L);
        when(administratorService.findByEmail("ada@example.com")).thenReturn(admin);
        String token = jwtTokenUtil.generateToken("ada@example.com", Rol.ADMINISTRADOR, 3L);

        assertThat(authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR).rol())
                .isEqualTo(Rol.ADMINISTRADOR);
    }

    @Test
    void autorizarConRolNoPermitidoLanzaExcepcionDeAutorizacion() {
        String token = jwtTokenUtil.generateToken("ana@example.com", Rol.ALUMNO, 9L);

        assertThatThrownBy(() -> authorizationService.autorizar(token, Rol.ADMINISTRADOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("No tiene permisos para esta operacion");
    }

    @Test
    void autorizarSinHeaderAuthorizationLanzaExcepcionDeAutenticacion() {
        assertThatThrownBy(() -> authorizationService.autorizar(null, Rol.INSTRUCTOR))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConTokenInvalidoLanzaExcepcionDeAutenticacion() {
        assertThatThrownBy(() -> authorizationService.autorizar("Bearer no-es-un-token", Rol.INSTRUCTOR))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConTokenDeOtraClaveLanzaExcepcionDeAutenticacion() {
        String ajeno = new JwtTokenUtil("otra-clave").generateToken("iris@example.com", Rol.INSTRUCTOR, 4L);

        assertThatThrownBy(() -> authorizationService.autorizar(ajeno, Rol.INSTRUCTOR))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token invalido o vencido");
    }

    @Test
    void autorizarConTokenSinRolLanzaExcepcionDeAutorizacion() {
        String sinRol = com.auth0.jwt.JWT.create().withSubject("iris@example.com")
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC512("clave-de-prueba"));

        assertThatThrownBy(() -> authorizationService.autorizar(sinRol, Rol.INSTRUCTOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El token no tiene rol asignado");
    }

    @Test
    void autorizarConUsuarioQueYaNoExisteLanzaExcepcionDeAutorizacion() {
        when(instructorService.findByEmail("borrado@example.com")).thenReturn(null);
        String token = jwtTokenUtil.generateToken("borrado@example.com", Rol.INSTRUCTOR, 4L);

        assertThatThrownBy(() -> authorizationService.autorizar(token, Rol.INSTRUCTOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El usuario del token ya no existe");
    }

    @Test
    void autorizarComoAlumnoBuscaElAlumno() {
        Student alumno = new Student("Ana", "Perez", "ana@example.com", "hash", "11111111");
        TestIds.fijarId(alumno, 9L);
        when(studentService.findByEmail("ana@example.com")).thenReturn(alumno);
        String token = jwtTokenUtil.generateToken("ana@example.com", Rol.ALUMNO, 9L);

        assertThat(authorizationService.autorizar(token, Rol.ALUMNO).id()).isEqualTo(9L);
    }
}
