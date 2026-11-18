package ar.edu.unnoba.pm2026.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /admin/auth y /admin/accounts (TP6, #39)
@WebMvcTest(AdminAccountController.class)
class AdminAccountControllerTest {

    private static final String BODY = """
            {"email":"eva@example.com","password":"secret","name":"Eva","lastName":"Sosa"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministratorService administratorService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    @Test
    void authDevuelveElTokenDelAdministrador() throws Exception {
        when(authenticationService.authenticate(any(Administrator.class))).thenReturn("Bearer jwt");

        mockMvc.perform(post("/admin/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"eva@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Bearer jwt"));
    }

    @Test
    void authConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(any(Administrator.class)))
                .thenThrow(new AuthenticationException("Email o contraseña incorrectos"));

        mockMvc.perform(post("/admin/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"eva@example.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication.failed"));
    }

    @Test
    void createResponde201ConLocationYLosCamposDelTp6() throws Exception {
        when(administratorService.create(any(Administrator.class))).thenReturn(Fixtures.administrator(5L));

        mockMvc.perform(post("/admin/accounts").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/administrators/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Eva"))
                .andExpect(jsonPath("$.lastName").value("Sosa"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createAceptaFirstNameComoNombre() throws Exception {
        when(administratorService.create(any(Administrator.class))).thenReturn(Fixtures.administrator(5L));

        mockMvc.perform(post("/admin/accounts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"eva@example.com","password":"secret","firstName":"Eva","lastName":"Sosa"}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<Administrator> created = ArgumentCaptor.forClass(Administrator.class);
        verify(administratorService).create(created.capture());
        assertThat(created.getValue().getFirstName()).isEqualTo("Eva");
    }

    @Test
    void createSinNombreResponde400ConElCampo() throws Exception {
        mockMvc.perform(post("/admin/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"eva@example.com\",\"password\":\"secret\",\"lastName\":\"Sosa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        verify(administratorService, never()).create(any());
    }

    @Test
    void createConEmailRepetidoResponde409() throws Exception {
        when(administratorService.create(any(Administrator.class))).thenThrow(new BusinessRuleException(
                "administrator.email-taken", "Ya existe un administrador con el email eva@example.com"));

        mockMvc.perform(post("/admin/accounts").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("administrator.email-taken"));
    }

    @Test
    void getAllDevuelveIdNombreApellidoYEmail() throws Exception {
        when(administratorService.getAll()).thenReturn(List.of(Fixtures.administrator(5L)));

        mockMvc.perform(get("/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].name").value("Eva"))
                .andExpect(jsonPath("$[0].lastName").value("Sosa"))
                .andExpect(jsonPath("$[0].email").value("eva@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void deleteResponde204() throws Exception {
        mockMvc.perform(delete("/admin/accounts/5")).andExpect(status().isNoContent());

        verify(administratorService).delete(5L, AuthTestSupport.ADMIN);
    }

    @Test
    void deleteDeSiMismoResponde409() throws Exception {
        doThrow(new BusinessRuleException("administrator.self-deletion",
                "Un administrador no puede eliminarse a si mismo"))
                .when(administratorService).delete(3L, AuthTestSupport.ADMIN);

        mockMvc.perform(delete("/admin/accounts/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("administrator.self-deletion"));
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        doThrow(new ResourceNotFoundException("administrator.not-found", "Administrador no encontrado con id 99"))
                .when(administratorService).delete(99L, AuthTestSupport.ADMIN);

        mockMvc.perform(delete("/admin/accounts/99")).andExpect(status().isNotFound());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get("/admin/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenDeInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);

        mockMvc.perform(post("/admin/accounts").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(administratorService, never()).create(any());
    }
}
