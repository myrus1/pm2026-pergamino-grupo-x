package ar.edu.unnoba.pm2026.enrollment.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentCourseResponse;
import ar.edu.unnoba.pm2026.enrollment.service.StudentCatalogService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /student/courses (TP6, #42)
@WebMvcTest(StudentCourseController.class)
class StudentCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentCatalogService studentCatalogService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asStudent(authorizationService, 1L);
    }

    private StudentCourseResponse course(boolean enrolled) {
        return new StudentCourseResponse(7L, "Curso de Java", "Desc", Fixtures.START, Fixtures.END, 5, 4,
                new BigDecimal("100.00"), new BigDecimal("75.00"), true, enrolled,
                new SpecialtyResponse(1L, "Programacion"),
                new CourseDetailResponse.CourseInstructor(2L, "Iris", "Ruiz", "iris@example.com"));
    }

    @Test
    void availableDevuelvePrecioBaseYPrecioConDescuento() throws Exception {
        when(studentCatalogService.getAvailableCourses(1L)).thenReturn(List.of(course(false)));

        mockMvc.perform(get("/student/courses/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].basePrice").value(100.00))
                .andExpect(jsonPath("$[0].applicablePrice").value(75.00))
                .andExpect(jsonPath("$[0].loyaltyDiscount").value(true));
    }

    @Test
    void getByIdIndicaCuposYSiYaEstaInscripto() throws Exception {
        when(studentCatalogService.getCourse(1L, 7L)).thenReturn(course(true));

        mockMvc.perform(get("/student/courses/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(4))
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.instructor.name").value("Iris"));
    }

    @Test
    void getByIdDeCursoNoPublicadoResponde404() throws Exception {
        when(studentCatalogService.getCourse(1L, 7L)).thenThrow(new ResourceNotFoundException("course.not-found",
                "Curso no encontrado con id 7"));

        mockMvc.perform(get("/student/courses/7")).andExpect(status().isNotFound());
    }

    @Test
    void conTokenDeInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 2L);

        mockMvc.perform(get("/student/courses/available")).andExpect(status().isForbidden());
        verify(studentCatalogService, never()).getAvailableCourses(2L);
    }
}
