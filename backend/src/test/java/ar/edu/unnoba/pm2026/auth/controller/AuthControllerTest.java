package ar.edu.unnoba.pm2026.auth.controller;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.dto.AuthenticationResult;
import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Login unico (#27): POST /api/auth/token
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String PATH = "/api/auth/token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void conCredencialesValidasDevuelveElTokenSinPrefijoYElRol() throws Exception {
        when(authenticationService.authenticate("ana@example.com", "secret"))
                .thenReturn(new AuthenticationResult("abc.def.ghi", Role.STUDENT, 864000));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("abc.def.ghi"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(864000))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void elMismoEndpointLogueaAInstructoresYAdministradores() throws Exception {
        when(authenticationService.authenticate("iris@example.com", "secret"))
                .thenReturn(new AuthenticationResult("i.n.s", Role.INSTRUCTOR, 864000));
        when(authenticationService.authenticate("ada@example.com", "secret"))
                .thenReturn(new AuthenticationResult("a.d.m", Role.ADMINISTRATOR, 864000));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"));
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ada@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"));
    }

    @Test
    void conCredencialesInvalidasResponde401ConProblemDetail() throws Exception {
        when(authenticationService.authenticate("ana@example.com", "otra"))
                .thenThrow(new AuthenticationException(ErrorCode.AUTHENTICATION_INVALID_CREDENTIALS));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"otra"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"))
                .andExpect(jsonPath("$.code").value("authentication.invalid-credentials"));
    }

    @Test
    void sinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El campo 'password' es obligatorio"));
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void conEmailMalFormadoResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"no-es-un-email","password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void sinBodyResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
