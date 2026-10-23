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

import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.SpecialtyService;
import com.example.demo.testutil.TestIds;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de SpecialtyController (slice web, sin contexto completo ni BD)
@WebMvcTest(SpecialtyController.class)
class SpecialtyControllerTest {

    private static final String RUTA = "/api/specialties";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecialtyService specialtyService;

    private Specialty especialidad() {
        Specialty especialidad = new Specialty("Programacion");
        TestIds.fijarId(especialidad, 1L);
        return especialidad;
    }

    @Test
    void getAllDevuelveLaListaDeEspecialidades() throws Exception {
        when(specialtyService.getAll()).thenReturn(List.of(especialidad()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Programacion"));
    }

    @Test
    void getByIdDevuelveLaEspecialidad() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(especialidad());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Programacion"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void createValidoResponde201ConLaEspecialidadCreada() throws Exception {
        when(specialtyService.save(any(Specialty.class))).thenAnswer(invocation -> {
            Specialty guardada = invocation.getArgument(0);
            TestIds.fijarId(guardada, 7L);
            return guardada;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Bases de datos"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Bases de datos"));
    }

    @Test
    void createSinNombreResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("name")));
    }

    @Test
    void updateCambiaElNombreDeLaEspecialidad() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(especialidad());
        when(specialtyService.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Algoritmos"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Algoritmos"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Algoritmos"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(especialidad());

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }
}
