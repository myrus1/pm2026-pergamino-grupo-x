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
import com.example.demo.user.model.Student;
import com.example.demo.user.services.StudentService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de StudentController (slice web, sin contexto completo ni BD)
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String RUTA = "/api/students";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    private Student alumno() {
        Student alumno = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        TestIds.fijarId(alumno, 1L);
        return alumno;
    }

    @Test
    void getAllDevuelveLaListaDeAlumnosSinPassword() throws Exception {
        when(studentService.getAll()).thenReturn(List.of(alumno()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].dni").value("11111111"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getByIdDevuelveElAlumno() throws Exception {
        when(studentService.getById(1L)).thenReturn(alumno());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElAlumnoCreado() throws Exception {
        when(studentService.save(any(Student.class))).thenAnswer(invocation -> {
            Student guardado = invocation.getArgument(0);
            TestIds.fijarId(guardado, 7L);
            return guardado;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.dni").value("11111111"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElAlumnoExistente() throws Exception {
        when(studentService.getById(1L)).thenReturn(alumno());
        when(studentService.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana Maria","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ana Maria"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(studentService.getById(1L)).thenReturn(alumno());

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }
}
