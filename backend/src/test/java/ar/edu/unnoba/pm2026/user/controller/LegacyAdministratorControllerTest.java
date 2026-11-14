package ar.edu.unnoba.pm2026.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Login del administrador (rol ADMINISTRADOR): POST /administrator/auth
@WebMvcTest(LegacyAdministratorController.class)
class LegacyAdministratorControllerTest {

    private static final String PATH = "/administrator/auth";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void authConCredencialesValidasDevuelveElTokenEnElBody() throws Exception {
        when(authenticationService.authenticate(any(Administrator.class))).thenReturn("Bearer abc.def.ghi");

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ada@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("Bearer abc.def.ghi"));

    }

    @Test
    void authConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(any(Administrator.class)))
                .thenThrow(new AuthenticationException("Email o contraseña incorrectos"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ada@example.com","password":"otra"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void authSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ada@example.com"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El campo 'password' es obligatorio"));
        verify(authenticationService, never()).authenticate(any(Administrator.class));
    }

    @Test
    void authSinEmailResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El campo 'email' es obligatorio"));
        verify(authenticationService, never()).authenticate(any(Administrator.class));
    }
}
