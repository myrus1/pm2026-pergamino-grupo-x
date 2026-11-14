package ar.edu.unnoba.pm2026.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LegacyInstructorController.class)
class LegacyInstructorControllerTest {

    private static final String PATH = "/instructor";
    private static final String TOKEN = "Bearer abc.def.ghi";
    private static final String BODY = """
            {"email":"iris@example.com","password":"secret","firstName":"Iris","lastName":"Ruiz"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private CourseService courseService;

    private void allowAsAdministrator() throws Exception {
        when(authorizationService.authorize(TOKEN, Role.ADMINISTRATOR))
                .thenReturn(new AuthenticatedUser(3L, "ada@example.com", Role.ADMINISTRATOR));
    }

    @Test
    void authConCredencialesValidasDevuelveElTokenEnElBody() throws Exception {
        when(authenticationService.authenticate(any(Instructor.class))).thenReturn("Bearer abc.def.ghi");

        mockMvc.perform(post(PATH + "/auth").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("Bearer abc.def.ghi"));

        ArgumentCaptor<Instructor> credentials = ArgumentCaptor.forClass(Instructor.class);
        verify(authenticationService).authenticate(credentials.capture());
        assertThat(credentials.getValue().getEmail()).isEqualTo("iris@example.com");
        assertThat(credentials.getValue().getPassword()).isEqualTo("secret");
    }

    @Test
    void authConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(any(Instructor.class)))
                .thenThrow(new AuthenticationException("Email o contraseña incorrectos"));

        mockMvc.perform(post(PATH + "/auth").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","password":"otra"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void authSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(PATH + "/auth").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com"}"""))
                .andExpect(status().isBadRequest());
        verify(authenticationService, never()).authenticate(any(Instructor.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/courses"})
    void getCoursesConTokenValidoDevuelveSoloLosCursosDelInstructor(String path) throws Exception {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "hash");
        TestIds.setId(instructor, 4L);
        Course course = new Course("Java", "Desde cero", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"), instructor);
        TestIds.setId(course, 7L);
        when(authorizationService.authorize(TOKEN, Role.INSTRUCTOR))
                .thenReturn(new AuthenticatedUser(4L, "iris@example.com", Role.INSTRUCTOR));
        when(courseService.getCoursesByInstructorId(4L)).thenReturn(List.of(course));

        mockMvc.perform(get(PATH + path).header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].description").value("Desde cero"));
    }

    @Test
    void getCoursesConTokenInvalidoResponde401() throws Exception {
        when(authorizationService.authorize("Bearer vencido", Role.INSTRUCTOR))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));

        mockMvc.perform(get(PATH + "/courses").header("Authorization", "Bearer vencido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(courseService, never()).getCoursesByInstructorId(any());
    }

    @Test
    void getCoursesSinHeaderAuthorizationResponde401() throws Exception {
        when(authorizationService.authorize(isNull(), eq(Role.INSTRUCTOR)))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));

        mockMvc.perform(get(PATH + "/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
    }

    @Test
    void getCoursesConRolQueNoEsInstructorResponde403() throws Exception {
        when(authorizationService.authorize(TOKEN, Role.INSTRUCTOR))
                .thenThrow(new AuthorizationException("No tiene permisos para esta operacion"));

        mockMvc.perform(get(PATH + "/courses").header("Authorization", TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(courseService, never()).getCoursesByInstructorId(any());
    }

    @Test
    void createComoAdministradorResponde201YDelegaEnElServicioConLosDatosDelDto() throws Exception {
        allowAsAdministrator();
        when(instructorService.create(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post(PATH).header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<Instructor> created = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorService).create(created.capture());
        assertThat(created.getValue().getEmail()).isEqualTo("iris@example.com");
        assertThat(created.getValue().getPassword()).isEqualTo("secret");
        assertThat(created.getValue().getFirstName()).isEqualTo("Iris");
        assertThat(created.getValue().getLastName()).isEqualTo("Ruiz");
    }

    @Test
    void createSinTokenResponde401SinLlamarAlServicio() throws Exception {
        when(authorizationService.authorize(isNull(), eq(Role.ADMINISTRATOR)))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(instructorService, never()).create(any());
    }

    @Test
    void createConRolQueNoEsAdministradorResponde403() throws Exception {
        when(authorizationService.authorize(TOKEN, Role.ADMINISTRATOR))
                .thenThrow(new AuthorizationException("No tiene permisos para esta operacion"));

        mockMvc.perform(post(PATH).header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(instructorService, never()).create(any());
    }

    @Test
    void createConEmailRepetidoResponde409() throws Exception {
        allowAsAdministrator();
        when(instructorService.create(any(Instructor.class)))
                .thenThrow(new InstructorEmailAlreadyExistsException("iris@example.com"));

        mockMvc.perform(post(PATH).header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un instructor con el email iris@example.com"));
    }

    @Test
    void createConEmailUsadoPorOtroUsuarioResponde409DesdeElRecurso() throws Exception {
        allowAsAdministrator();
        when(instructorService.create(any(Instructor.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        mockMvc.perform(post(PATH).header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario con el email iris@example.com"));
    }

    @Test
    void createSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        allowAsAdministrator();

        mockMvc.perform(post(PATH).header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","firstName":"Iris","lastName":"Ruiz"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("password")));
        verify(instructorService, never()).create(any());
    }
}
