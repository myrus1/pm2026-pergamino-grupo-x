package com.example.demo.user.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InstructorResource.class)
class InstructorResourceTest {

    private static final String RUTA = "/instructor";
    private static final String CUERPO = """
            {"email":"iris@example.com","password":"secret","firstName":"Iris","lastName":"Ruiz"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    @Test
    void createResponde201YDelegaEnElServicioConLosDatosDelDto() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<Instructor> creado = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorService).create(creado.capture());
        assertThat(creado.getValue().getEmail()).isEqualTo("iris@example.com");
        assertThat(creado.getValue().getPassword()).isEqualTo("secret");
        assertThat(creado.getValue().getFirstName()).isEqualTo("Iris");
        assertThat(creado.getValue().getLastName()).isEqualTo("Ruiz");
    }

    @Test
    void createConEmailRepetidoResponde409() throws Exception {
        when(instructorService.create(any(Instructor.class)))
                .thenThrow(new InstructorEmailAlreadyExistsException("iris@example.com"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un instructor con el email iris@example.com"));
    }

    @Test
    void createConEmailUsadoPorOtroUsuarioResponde409DesdeElRecurso() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario con el email iris@example.com"));
    }

    @Test
    void createSinPasswordResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"iris@example.com","firstName":"Iris","lastName":"Ruiz"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("password")));
        verify(instructorService, never()).create(any());
    }
}
