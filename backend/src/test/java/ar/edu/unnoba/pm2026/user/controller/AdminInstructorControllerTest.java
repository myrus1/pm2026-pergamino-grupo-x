package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.user.dto.InstructorDetailResponse;
import ar.edu.unnoba.pm2026.user.dto.InstructorSummaryResponse;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /admin/instructors (TP6, #39)
@WebMvcTest(AdminInstructorController.class)
class AdminInstructorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    @Test
    void getDevuelveLosInstructoresConSusEspecialidades() throws Exception {
        when(instructorService.getSummaries()).thenReturn(List.of(new InstructorSummaryResponse(2L, "Iris", "Ruiz",
                "iris@example.com", List.of(new SpecialtyResponse(1L, "Programacion")))));

        mockMvc.perform(get("/admin/instructors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Iris"))
                .andExpect(jsonPath("$[0].specialties[0].name").value("Programacion"));
    }

    @Test
    void getByIdDevuelveElResumenDeCursos() throws Exception {
        when(instructorService.getDetail(2L)).thenReturn(new InstructorDetailResponse(2L, "Iris", "Ruiz",
                "iris@example.com", List.of(), new InstructorDetailResponse.CourseSummary(2, 1, 0, 1),
                List.of(CourseResponse.from(Fixtures.course(7L, CourseStatus.PUBLISHED)))));

        mockMvc.perform(get("/admin/instructors/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseSummary.total").value(2))
                .andExpect(jsonPath("$.courseSummary.published").value(1))
                .andExpect(jsonPath("$.courses[0].id").value(7));
    }

    @Test
    void getByIdInexistenteResponde404() throws Exception {
        when(instructorService.getDetail(99L)).thenThrow(new ResourceNotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND, 99L));

        mockMvc.perform(get("/admin/instructors/99")).andExpect(status().isNotFound());
    }

    @Test
    void getCoursesDevuelveSoloLosPublicados() throws Exception {
        when(courseService.getByInstructorAndStatus(2L, CourseStatus.PUBLISHED))
                .thenReturn(List.of(Fixtures.course(7L, CourseStatus.PUBLISHED)));

        mockMvc.perform(get("/admin/instructors/2/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    void getPendingDevuelveLosPendientesDeAprobacion() throws Exception {
        when(courseService.getByInstructorAndStatus(2L, CourseStatus.PENDING_APPROVAL))
                .thenReturn(List.of(Fixtures.course(9L, CourseStatus.PENDING_APPROVAL)));

        mockMvc.perform(get("/admin/instructors/2/courses/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"));
    }

    @Test
    void getCoursesDeInstructorInexistenteResponde404() throws Exception {
        when(courseService.getByInstructorAndStatus(99L, CourseStatus.PUBLISHED))
                .thenThrow(new ResourceNotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND, 99L));

        mockMvc.perform(get("/admin/instructors/99/courses")).andExpect(status().isNotFound());
    }

    @Test
    void conTokenDeInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 2L);

        mockMvc.perform(get("/admin/instructors/2/courses/pending")).andExpect(status().isForbidden());
        verify(courseService, never()).getByInstructorAndStatus(any(), any());
    }
}
