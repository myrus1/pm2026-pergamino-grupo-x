package com.example.demo.user.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.CourseService;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
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

@WebMvcTest(InstructorResource.class)
class InstructorResourceTest {

    private static final String RUTA = "/instructor";
    private static final String CUERPO = """
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

    @Test
    void authConCredencialesValidasDevuelveElTokenEnElBody() throws Exception {
        when(authenticationService.authenticate(any(Instructor.class))).thenReturn("Bearer abc.def.ghi");

        mockMvc.perform(post(RUTA + "/auth").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("Bearer abc.def.ghi"));

        ArgumentCaptor<Instructor> credenciales = ArgumentCaptor.forClass(Instructor.class);
        verify(authenticationService).authenticate(credenciales.capture());
        assertThat(credenciales.getValue().getEmail()).isEqualTo("iris@example.com");
        assertThat(credenciales.getValue().getPassword()).isEqualTo("secret");
    }

    @Test
    void authConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(any(Instructor.class)))
                .thenThrow(new AuthenticationException("Email o contraseña incorrectos"));

        mockMvc.perform(post(RUTA + "/auth").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","password":"otra"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void authSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(RUTA + "/auth").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com"}"""))
                .andExpect(status().isBadRequest());
        verify(authenticationService, never()).authenticate(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/courses"})
    void getCoursesConTokenValidoDevuelveSoloLosCursosDelInstructor(String ruta) throws Exception {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "hash");
        TestIds.fijarId(instructor, 4L);
        Course curso = new Course("Java", "Desde cero", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"), instructor);
        TestIds.fijarId(curso, 7L);
        when(authorizationService.authorize("Bearer abc.def.ghi")).thenReturn(instructor);
        when(courseService.getCoursesByInstructorId(4L)).thenReturn(List.of(curso));

        mockMvc.perform(get(RUTA + ruta).header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].description").value("Desde cero"));
    }

    @Test
    void getCoursesConTokenInvalidoResponde403() throws Exception {
        when(authorizationService.authorize("Bearer vencido"))
                .thenThrow(new AuthorizationException("Token invalido o vencido"));

        mockMvc.perform(get(RUTA + "/courses").header("Authorization", "Bearer vencido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(courseService, never()).getCoursesByInstructorId(any());
    }

    @Test
    void getCoursesSinHeaderAuthorizationResponde403() throws Exception {
        when(authorizationService.authorize(isNull())).thenThrow(new AuthorizationException("Token invalido o vencido"));

        mockMvc.perform(get(RUTA + "/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResponde201YDelegaEnElServicioConLosDatosDelDto() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<Instructor> creado = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorService).create(creado.capture());
        assertThat(creado.getValue().getEmail()).isEqualTo("iris@example.com");
        assertThat(creado.getValue().getPassword()).isEqualTo("secret");
        assertThat(creado.getValue().getFirstName()).isEqualTo("Iris");
        assertThat(creado.getValue().getLastName()).isEqualTo("Ruiz");
    }

    @Test
    void createConEmailRepetidoResponde409() throws Exception {
        when(instructorService.create(any(Instructor.class)))
                .thenThrow(new InstructorEmailAlreadyExistsException("iris@example.com"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un instructor con el email iris@example.com"));
    }

    @Test
    void createConEmailUsadoPorOtroUsuarioResponde409DesdeElRecurso() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario con el email iris@example.com"));
    }

    @Test
    void createSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","firstName":"Iris","lastName":"Ruiz"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("password")));
        verify(instructorService, never()).create(any());
    }
}
