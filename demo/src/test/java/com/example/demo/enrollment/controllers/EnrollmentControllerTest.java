package com.example.demo.enrollment.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.enrollment.model.CourseEnrollmentException;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.services.EnrollmentService;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
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

// Contrato HTTP de EnrollmentController (slice web, sin contexto completo ni BD)
@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    private static final String RUTA = "/api/enrollments";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    private Student alumno() {
        Student alumno = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        TestIds.fijarId(alumno, 1L);
        return alumno;
    }

    // Curso publicado que comienza en el futuro (requisito de Student.applyCourse)
    private Course cursoPublicado() {
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.now().plusWeeks(2),
                LocalDate.now().plusWeeks(6), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        curso.setStatus(CourseStatus.PUBLISHED);
        TestIds.fijarId(curso, 2L);
        return curso;
    }

    private Enrollment inscripcion() {
        Enrollment inscripcion = new Enrollment(alumno(), cursoPublicado(), new BigDecimal("100.00"));
        TestIds.fijarId(inscripcion, 9L);
        return inscripcion;
    }

    @Test
    void getAllDevuelveLaListaDeInscripcionesConLosIdsRelacionados() throws Exception {
        when(enrollmentService.getAll()).thenReturn(List.of(inscripcion()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].courseId").value(2));
    }

    @Test
    void getByIdDevuelveLaInscripcion() throws Exception {
        when(enrollmentService.getById(9L)).thenReturn(inscripcion());

        mockMvc.perform(get(RUTA + "/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(enrollmentService.getById(99L)).thenThrow(new EntityNotFoundException("Inscripcion no encontrada con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Inscripcion no encontrada con id 99"));
    }

    @Test
    void createValidoResponde201ConLaInscripcionGenerada() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenReturn(inscripcion());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":1,"courseId":2}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.courseId").value(2))
                .andExpect(content().string(containsString("\"finalAmount\":100.00")))
                .andExpect(jsonPath("$.enrollmentDate").exists());
    }

    @Test
    void createConCursoNoPublicadoResponde409() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenThrow(new CourseEnrollmentException("El curso no esta publicado"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":1,"courseId":2}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El curso no esta publicado"));
    }

    @Test
    void createConAlumnoInexistenteResponde404() throws Exception {
        when(enrollmentService.enroll(99L, 2L))
                .thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":99,"courseId":2}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void createSinIdsResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("studentId")));
    }

    @Test
    void revenueDevuelveElTotalRecaudado() throws Exception {
        when(enrollmentService.totalRevenue()).thenReturn(new BigDecimal("160.00"));

        mockMvc.perform(get(RUTA + "/revenue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\":160.00")));
    }

    @Test
    void updateNoEstaPermitidoResponde405() throws Exception {
        mockMvc.perform(put(RUTA + "/9").contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":1,"courseId":2}"""))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void cancelarUnaInscripcionNoEstaPermitidoResponde405() throws Exception {
        mockMvc.perform(delete(RUTA + "/9"))
                .andExpect(status().isMethodNotAllowed());
    }
}
