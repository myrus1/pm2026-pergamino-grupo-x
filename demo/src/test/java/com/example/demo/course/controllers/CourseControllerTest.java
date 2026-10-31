package com.example.demo.course.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.CourseService;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Instructor;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de CourseController (slice web, sin contexto completo ni BD)
@WebMvcTest(CourseController.class)
class CourseControllerTest {

    private static final String RUTA = "/api/courses";
    private static final String CUERPO = """
            {"title":"Curso de Java","description":"Desc","basePrice":100.00,
             "startDate":"2026-06-01","endDate":"2026-06-20","maxCapacity":5,
             "specialtyId":1,"instructorId":2}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    private Specialty especialidad() {
        Specialty especialidad = new Specialty("Programacion");
        TestIds.fijarId(especialidad, 1L);
        return especialidad;
    }

    private Instructor instructor() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.fijarId(instructor, 2L);
        return instructor;
    }

    private Course curso() {
        Course curso = new Course("Curso de Java", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, especialidad(), instructor());
        TestIds.fijarId(curso, 5L);
        return curso;
    }

    @Test
    void getAllDevuelveLosCursosConEspecialidadesEInstructoresPorId() throws Exception {
        when(courseService.getAll()).thenReturn(List.of(curso()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].specialtyId").value(1))
                .andExpect(jsonPath("$[0].instructorId").value(2));
    }

    @Test
    void getByIdDevuelveElCurso() throws Exception {
        when(courseService.getById(5L)).thenReturn(curso());

        mockMvc.perform(get(RUTA + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Curso de Java"))
                .andExpect(content().string(containsString("\"basePrice\":100.00")));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(courseService.getById(99L)).thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElCursoCreado() throws Exception {
        Course creado = new Course("Curso de Java", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, especialidad(), instructor());
        TestIds.fijarId(creado, 7L);
        when(courseService.create(any(CourseRequest.class))).thenReturn(creado);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.specialtyId").value(1))
                .andExpect(jsonPath("$.instructorId").value(2));
    }

    @Test
    void createSinTituloResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Desc","basePrice":100.00,"startDate":"2026-06-01",
                 "endDate":"2026-06-20","maxCapacity":5,"specialtyId":1,"instructorId":2}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("title")));
    }

    @Test
    void createConEspecialidadInexistenteResponde404() throws Exception {
        when(courseService.create(any(CourseRequest.class)))
                .thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 1"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 1"));
    }

    @Test
    void updateModificaElCursoYPermiteCambiarElEstado() throws Exception {
        Course actualizado = new Course("Curso de Java Avanzado", "Desc", new BigDecimal("100.00"),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20), 5, especialidad(), instructor());
        actualizado.setStatus(CourseStatus.PUBLISHED);
        TestIds.fijarId(actualizado, 5L);
        when(courseService.update(eq(5L), any(CourseRequest.class))).thenReturn(actualizado);

        mockMvc.perform(put(RUTA + "/5").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Curso de Java Avanzado","description":"Desc","basePrice":100.00,
                 "startDate":"2026-06-01","endDate":"2026-06-20","maxCapacity":5,
                 "specialtyId":1,"instructorId":2,"status":"PUBLISHED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Curso de Java Avanzado"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(courseService.update(eq(99L), any(CourseRequest.class)))
                .thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(courseService.getById(5L)).thenReturn(curso());

        mockMvc.perform(delete(RUTA + "/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(courseService.getById(99L)).thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }
}
