package com.example.demo.controllers;

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

import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.services.AdministratorService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de AdministratorController (slice web, sin contexto completo ni BD)
@WebMvcTest(AdministratorController.class)
class AdministratorControllerTest {

    private static final String RUTA = "/api/administrators";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministratorService administratorService;

    private Administrator administrador() {
        Administrator administrador = new Administrator("Eva", "Sosa", "eva@example.com", "secret");
        TestIds.fijarId(administrador, 1L);
        return administrador;
    }

    @Test
    void getAllDevuelveLaListaDeAdministradoresSinPassword() throws Exception {
        when(administratorService.getAll()).thenReturn(List.of(administrador()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getByIdDevuelveElAdministrador() throws Exception {
        when(administratorService.getById(1L)).thenReturn(administrador());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("eva@example.com"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(administratorService.getById(99L)).thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Administrador no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElAdministradorCreado() throws Exception {
        when(administratorService.save(any(Administrator.class))).thenAnswer(invocation -> {
            Administrator guardado = invocation.getArgument(0);
            TestIds.fijarId(guardado, 7L);
            return guardado;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Eva","lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElAdministradorExistente() throws Exception {
        when(administratorService.getById(1L)).thenReturn(administrador());
        when(administratorService.save(any(Administrator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Eva Maria","lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Eva Maria"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(administratorService.getById(99L)).thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Eva","lastName":"Sosa","email":"eva@example.com","password":"secret"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Administrador no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(administratorService.getById(1L)).thenReturn(administrador());

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(administratorService.getById(99L)).thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Administrador no encontrado con id 99"));
    }
}
