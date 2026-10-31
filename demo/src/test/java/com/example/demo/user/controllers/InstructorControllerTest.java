package com.example.demo.user.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de InstructorController (slice web, sin contexto completo ni BD)
@WebMvcTest(InstructorController.class)
class InstructorControllerTest {

    private static final String RUTA = "/api/instructors";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    private Instructor instructor() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.fijarId(instructor, 1L);
        return instructor;
    }

    @Test
    void getAllDevuelveLaListaDeInstructoresSinPassword() throws Exception {
        when(instructorService.getAll()).thenReturn(List.of(instructor()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("iris@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getByIdDevuelveElInstructor() throws Exception {
        when(instructorService.getById(1L)).thenReturn(instructor());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Iris"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElInstructorCreado() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenAnswer(invocation -> {
            Instructor guardado = invocation.getArgument(0);
            TestIds.fijarId(guardado, 7L);
            return guardado;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createConEmailExistenteResponde409() throws Exception {
        when(instructorService.create(any(Instructor.class)))
                .thenThrow(new InstructorEmailAlreadyExistsException("iris@example.com"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un instructor con el email iris@example.com"));
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElInstructorExistente() throws Exception {
        when(instructorService.getById(1L)).thenReturn(instructor());
        when(instructorService.update(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris Maria","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Iris Maria"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(instructorService.getById(1L)).thenReturn(instructor());

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void asignarEspecialidadDelegaEnElServicioYResponde204() throws Exception {
        mockMvc.perform(put(RUTA + "/1/specialties/2"))
                .andExpect(status().isNoContent());

        verify(instructorService).addSpecialty(1L, 2L);
    }

    @Test
    void asignarEspecialidadInexistenteResponde404() throws Exception {
        doThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"))
                .when(instructorService).addSpecialty(1L, 99L);

        mockMvc.perform(put(RUTA + "/1/specialties/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void quitarEspecialidadDelegaEnElServicioYResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/1/specialties/2"))
                .andExpect(status().isNoContent());

        verify(instructorService).removeSpecialty(1L, 2L);
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }
}
