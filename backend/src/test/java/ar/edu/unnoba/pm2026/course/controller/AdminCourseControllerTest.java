package ar.edu.unnoba.pm2026.course.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrolledStudentResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /admin/courses (TP6, #39)
@WebMvcTest(AdminCourseController.class)
class AdminCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    @Test
    void getDevuelveLosCursosPublicadosEnElOrdenDelServicio() throws Exception {
        when(courseService.getPublished()).thenReturn(List.of(Fixtures.course(8L, CourseStatus.PUBLISHED),
                Fixtures.course(7L, CourseStatus.PUBLISHED)));

        mockMvc.perform(get("/admin/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[1].id").value(7))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    void getByIdDevuelveElDetalleConInstructorInscriptosYCupo() throws Exception {
        when(courseService.getDetail(7L)).thenReturn(new CourseDetailResponse(7L, "Curso de Java", "Desc",
                CourseStatus.PUBLISHED, Fixtures.START, Fixtures.END, new BigDecimal("100.00"), 5, 2, 3,
                new SpecialtyResponse(1L, "Programacion"),
                new CourseDetailResponse.CourseInstructor(2L, "Iris", "Ruiz", "iris@example.com")));

        mockMvc.perform(get("/admin/courses/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCapacity").value(5))
                .andExpect(jsonPath("$.enrolledStudents").value(2))
                .andExpect(jsonPath("$.availableSeats").value(3))
                .andExpect(jsonPath("$.instructor.name").value("Iris"))
                .andExpect(jsonPath("$.specialty.name").value("Programacion"));
    }

    @Test
    void getByIdInexistenteResponde404() throws Exception {
        when(courseService.getDetail(99L)).thenThrow(new ResourceNotFoundException("course.not-found",
                "Curso no encontrado con id 99"));

        mockMvc.perform(get("/admin/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("course.not-found"));
    }

    @Test
    void getStudentsDevuelveFechaDeInscripcionYPrecioPagado() throws Exception {
        when(enrollmentService.getEnrolledStudents(7L)).thenReturn(List.of(new EnrolledStudentResponse(11L, 1L,
                "Ana", "Diaz", "ana1@example.com", "30000001", LocalDateTime.of(2026, 5, 10, 0, 0),
                new BigDecimal("75.00"))));

        mockMvc.perform(get("/admin/courses/7/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].enrollmentDate").value("2026-05-10T00:00:00"))
                .andExpect(jsonPath("$[0].finalAmount").value(75.00));
    }

    @Test
    void getStudentsDeCursoInexistenteResponde404() throws Exception {
        when(enrollmentService.getEnrolledStudents(99L)).thenThrow(new ResourceNotFoundException(
                "Curso no encontrado con id 99"));

        mockMvc.perform(get("/admin/courses/99/students")).andExpect(status().isNotFound());
    }

    @Test
    void conTokenDeAlumnoResponde403() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 1L);

        mockMvc.perform(get("/admin/courses")).andExpect(status().isForbidden());
        verify(courseService, never()).getPublished();
    }

    @Test
    void sinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get("/admin/courses/7")).andExpect(status().isUnauthorized());
    }
}
