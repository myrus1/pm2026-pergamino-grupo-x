package ar.edu.unnoba.pm2026.user.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.user.dto.StudentDetailResponse;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /admin/students (TP6, #39)
@WebMvcTest(AdminStudentController.class)
class AdminStudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    @Test
    void getDevuelveLosAlumnosSinPassword() throws Exception {
        when(studentService.getAll()).thenReturn(List.of(Fixtures.student(1L)));

        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].dni").value("30000001"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getByIdDevuelveElDetalleConLoPagado() throws Exception {
        when(studentService.getDetail(1L)).thenReturn(new StudentDetailResponse(1L, "Ana", "Diaz",
                "ana1@example.com", "30000001", 2, new BigDecimal("175.00")));

        mockMvc.perform(get("/admin/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCourses").value(2))
                .andExpect(jsonPath("$.totalPaid").value(175.00));
    }

    @Test
    void getByIdInexistenteResponde404() throws Exception {
        when(studentService.getDetail(99L)).thenThrow(new ResourceNotFoundException("student.not-found",
                "Estudiante no encontrado con id 99"));

        mockMvc.perform(get("/admin/students/99")).andExpect(status().isNotFound());
    }

    @Test
    void getCoursesDevuelveLosCursosDelAlumno() throws Exception {
        when(enrollmentService.getCoursesOfStudent(1L)).thenReturn(List.of(Fixtures.course(7L, CourseStatus.PUBLISHED)));

        mockMvc.perform(get("/admin/students/1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void getCoursesDeAlumnoInexistenteResponde404() throws Exception {
        when(enrollmentService.getCoursesOfStudent(99L)).thenThrow(new ResourceNotFoundException(
                "Estudiante no encontrado con id 99"));

        mockMvc.perform(get("/admin/students/99/courses")).andExpect(status().isNotFound());
    }

    @Test
    void conTokenDeAlumnoResponde403() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 1L);

        mockMvc.perform(get("/admin/students/1")).andExpect(status().isForbidden());
        verify(studentService, never()).getDetail(1L);
    }
}
