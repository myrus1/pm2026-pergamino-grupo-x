package ar.edu.unnoba.pm2026.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de POST /students y POST /students/auth (TP6, #42): los dos son publicos
@WebMvcTest(StudentAccountController.class)
class StudentAccountControllerTest {

    private static final String BODY = """
            {"name":"Ana","lastName":"Diaz","dni":"30000001","email":"ana@example.com","password":"secret"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void registerConLosCamposDelTp6Responde201() throws Exception {
        when(studentService.create(any(Student.class))).thenAnswer(invocation -> {
            Student created = invocation.getArgument(0);
            TestIds.setId(created, 1L);
            return created;
        });

        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/students/1"))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.dni").value("30000001"))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<Student> created = ArgumentCaptor.forClass(Student.class);
        verify(studentService).create(created.capture());
        assertThat(created.getValue().getFirstName()).isEqualTo("Ana");
        assertThat(created.getValue().getDni()).isEqualTo("30000001");
    }

    @Test
    void registerSinDniResponde400ConElCampo() throws Exception {
        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace(",\"dni\":\"30000001\"", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dni"));
        verify(studentService, never()).create(any());
    }

    @Test
    void registerConEmailODniRepetidoResponde409() throws Exception {
        when(studentService.create(any(Student.class))).thenThrow(
                new DataIntegrityViolationException("duplicado", new SQLException("duplicado", "23505")));

        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data.integrity.conflict"));
    }

    @Test
    void authDevuelveElTokenDelAlumno() throws Exception {
        when(authenticationService.authenticate(any(Student.class))).thenReturn("Bearer jwt");

        mockMvc.perform(post("/students/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Bearer jwt"));
    }

    @Test
    void authConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(any(Student.class)))
                .thenThrow(new AuthenticationException("Email o contraseña incorrectos"));

        mockMvc.perform(post("/students/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }
}
