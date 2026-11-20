package ar.edu.unnoba.pm2026.enrollment.controller;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentEnrollmentResponse;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de las inscripciones del alumno (TP6, #42)
@WebMvcTest(StudentEnrollmentController.class)
class StudentEnrollmentControllerTest {

    private static final AuthenticatedUser ANA = new AuthenticatedUser(1L, "ana@example.com", Role.STUDENT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.as(authorizationService, ANA);
    }

    private StudentEnrollmentResponse enrollment() {
        return new StudentEnrollmentResponse(11L, LocalDateTime.of(2026, 5, 10, 0, 0), new BigDecimal("100.00"),
                new BigDecimal("75.00"), new BigDecimal("25.00"), new StudentEnrollmentResponse.EnrolledCourse(7L,
                        "Curso de Java", "Desc", Fixtures.START, Fixtures.END, "Programacion", "Iris Ruiz"));
    }

    @Test
    void enrollResponde201ConPrecioBaseFinalYDescuento() throws Exception {
        when(enrollmentService.enrollStudent(7L, ANA)).thenReturn(enrollment());

        mockMvc.perform(post("/student/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":7}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/enrollments/11"))
                .andExpect(jsonPath("$.basePrice").value(100.00))
                .andExpect(jsonPath("$.finalAmount").value(75.00))
                .andExpect(jsonPath("$.discount").value(25.00))
                .andExpect(jsonPath("$.course.title").value("Curso de Java"));
    }

    @Test
    void enrollSinCursoResponde400() throws Exception {
        mockMvc.perform(post("/student/enrollments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("courseId"));
        verify(enrollmentService, never()).enrollStudent(any(), any());
    }

    @Test
    void enrollAUnCursoLlenoResponde409() throws Exception {
        when(enrollmentService.enrollStudent(7L, ANA))
                .thenThrow(new CourseEnrollmentException(ErrorCode.ENROLLMENT_COURSE_FULL));

        mockMvc.perform(post("/student/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":7}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("enrollment.course-full"));
    }

    @Test
    void enrollAUnCursoInexistenteResponde404() throws Exception {
        when(enrollmentService.enrollStudent(99L, ANA)).thenThrow(new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND, 99L));

        mockMvc.perform(post("/student/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":99}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void enrollComoAdministradorResponde403() throws Exception {
        AuthTestSupport.asDefaultAdministrator(authorizationService);

        mockMvc.perform(post("/student/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":7}"))
                .andExpect(status().isForbidden());
        verify(enrollmentService, never()).enrollStudent(any(), any());
    }

    @Test
    void getDevuelveSusInscripciones() throws Exception {
        when(enrollmentService.getStudentEnrollments(1L)).thenReturn(List.of(enrollment()));

        mockMvc.perform(get("/students/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(11))
                .andExpect(jsonPath("$[0].course.instructor").value("Iris Ruiz"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get("/students/enrollments")).andExpect(status().isUnauthorized());
    }
}
