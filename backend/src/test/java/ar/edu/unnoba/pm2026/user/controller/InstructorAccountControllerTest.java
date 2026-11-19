package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de POST /instructor y POST /auth (TP6, #41): los dos son publicos
@WebMvcTest(InstructorAccountController.class)
class InstructorAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    @MockitoBean
    private AuthenticationService authenticationService;

    private void createReturnsWithId(Long id) {
        when(instructorService.create(any(Instructor.class))).thenAnswer(invocation -> {
            Instructor created = invocation.getArgument(0);
            TestIds.setId(created, id);
            return created;
        });
    }

    private Instructor createdInstructor() {
        ArgumentCaptor<Instructor> created = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorService).create(created.capture());
        return created.getValue();
    }

    @Test
    void registerSinTokenConEmailYPasswordResponde201() throws Exception {
        createReturnsWithId(7L);

        mockMvc.perform(post("/instructor").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"iris@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/instructors/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("iris@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Instructor created = createdInstructor();
        assertThat(created.getPassword()).isEqualTo("secret");
        assertThat(created.getFirstName()).isEmpty();
        assertThat(created.getLastName()).isEmpty();
    }

    @Test
    void registerGuardaNombreYApellidoSiVienen() throws Exception {
        createReturnsWithId(7L);

        mockMvc.perform(post("/instructor").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"iris@example.com","password":"secret","firstName":"Iris","lastName":"Ruiz"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Iris"));

        assertThat(createdInstructor().getLastName()).isEqualTo("Ruiz");
    }

    @Test
    void registerConEmailRepetidoResponde409() throws Exception {
        when(instructorService.create(any(Instructor.class)))
                .thenThrow(new InstructorEmailAlreadyExistsException("iris@example.com"));

        mockMvc.perform(post("/instructor").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"iris@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un instructor con el email iris@example.com"));
    }

    @Test
    void registerConEmailDeOtroUsuarioResponde409DesdeLaBase() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenThrow(
                new DataIntegrityViolationException("duplicado", new SQLException("duplicado", "23505")));

        mockMvc.perform(post("/instructor").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post("/instructor").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"iris@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("password")));
        verify(instructorService, never()).create(any());
    }

    @Test
    void authEnLaRaizDevuelveElTokenDelInstructor() throws Exception {
        when(authenticationService.authenticate(any(Instructor.class))).thenReturn("Bearer jwt");

        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"iris@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Bearer jwt"));
    }

    @Test
    void authConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(any(Instructor.class)))
                .thenThrow(new AuthenticationException(ErrorCode.AUTHENTICATION_INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"iris@example.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }
}
