package ar.edu.unnoba.pm2026.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de AdministratorController (slice web, sin contexto completo ni BD)
@WebMvcTest(AdministratorController.class)
class AdministratorControllerTest {

    private static final String PATH = "/api/administrators";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministratorService administratorService;
    private static final String BODY = """
            {"firstName":"Eva","lastName":"Sosa","email":"eva@example.com","password":"secret"}""";

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    private Administrator administrator() {
        Administrator administrator = new Administrator("Eva", "Sosa", "eva@example.com", "secret");
        TestIds.setId(administrator, 1L);
        return administrator;
    }

    @Test
    void getAllDevuelveLaListaDeAdministradoresSinPassword() throws Exception {
        when(administratorService.getAll()).thenReturn(List.of(administrator()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getByIdDevuelveElAdministrador() throws Exception {
        when(administratorService.getById(1L)).thenReturn(administrator());

        mockMvc.perform(get(PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("eva@example.com"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(administratorService.getById(99L)).thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        mockMvc.perform(get(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Administrador no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElAdministradorCreado() throws Exception {
        when(administratorService.create(any(Administrator.class))).thenAnswer(invocation -> {
            Administrator saved = invocation.getArgument(0);
            TestIds.setId(saved, 7L);
            return saved;
        });

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Eva","lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElAdministradorExistente() throws Exception {
        when(administratorService.getById(1L)).thenReturn(administrator());
        when(administratorService.update(any(Administrator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Eva Maria","lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Eva Maria"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(administratorService.getById(99L)).thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        mockMvc.perform(put(PATH + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Eva","lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Administrador no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(administratorService.getById(1L)).thenReturn(administrator());

        mockMvc.perform(delete(PATH + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(administratorService.getById(99L)).thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        mockMvc.perform(delete(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Administrador no encontrado con id 99"));
    }

    @Test
    void getAllSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(administratorService, never()).getAll();
    }

    @Test
    void getByIdSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH + "/1")).andExpect(status().isUnauthorized());
        verify(administratorService, never()).getById(any());
    }

    @Test
    void createSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(administratorService, never()).create(any());
    }

    @Test
    void createConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(administratorService, never()).create(any());
    }

    @Test
    void updateSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(administratorService, never()).update(any());
    }

    @Test
    void deleteSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(delete(PATH + "/1")).andExpect(status().isUnauthorized());
        verify(administratorService, never()).delete(any());
    }
}
