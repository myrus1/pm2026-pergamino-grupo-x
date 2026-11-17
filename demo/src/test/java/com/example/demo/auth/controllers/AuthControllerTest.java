package com.example.demo.auth.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.dto.AuthenticationResult;
import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.auth.services.AuthenticationService;
import com.example.demo.common.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Login unico: POST /api/auth/token
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String RUTA = "/api/auth/token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    private void prepararLogin(Rol rol) {
        when(authenticationService.authenticate("usuario@example.com", "secret"))
                .thenReturn(new AuthenticationResult("abc.def.ghi", rol, 864000));
    }

    private void esperarToken(Rol rol) throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("abc.def.ghi"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(864000))
                .andExpect(jsonPath("$.role").value(rol.name()));
    }

    @Test
    void tokenDeAlumnoDevuelveElRolStudent() throws Exception {
        prepararLogin(Rol.STUDENT);
        esperarToken(Rol.STUDENT);
    }

    @Test
    void tokenDeInstructorDevuelveElRolInstructor() throws Exception {
        prepararLogin(Rol.INSTRUCTOR);
        esperarToken(Rol.INSTRUCTOR);
    }

    @Test
    void tokenDeAdministradorDevuelveElRolAdministrator() throws Exception {
        prepararLogin(Rol.ADMINISTRATOR);
        esperarToken(Rol.ADMINISTRATOR);
    }

    @Test
    void tokenConCredencialesInvalidasResponde401() throws Exception {
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Email o contraseña incorrectos"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":"otra"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void tokenSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El campo 'password' es obligatorio"));
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void tokenSinEmailResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El campo 'email' es obligatorio"));
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }
}
