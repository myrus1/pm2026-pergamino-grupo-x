package ar.edu.unnoba.pm2026.course.controller;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.dto.UpdateCourseRequest;
import ar.edu.unnoba.pm2026.course.exception.CourseStateException;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrolledStudentResponse;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /instructor/courses (TP6, #41)
@WebMvcTest(InstructorCourseController.class)
class InstructorCourseControllerTest {

    private static final String PATH = "/instructor/courses";
    private static final AuthenticatedUser IRIS = new AuthenticatedUser(2L, "iris@example.com", Role.INSTRUCTOR);
    private static final String BODY = """
            {"title":"Curso de Java","description":"Desc","startDate":"2026-06-01","endDate":"2026-06-20",
             "maxCapacity":5,"basePrice":100.00,"specialty":1}""";

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
        AuthTestSupport.as(authorizationService, IRIS);
    }

    @Test
    void getDevuelveIdNameYDescriptionDeSusCursos() throws Exception {
        when(courseService.getCoursesByInstructorId(2L)).thenReturn(List.of(Fixtures.course(7L, CourseStatus.DRAFT)));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Curso de Java"))
                .andExpect(jsonPath("$[0].description").value("Desc"));
    }

    @Test
    void getByIdDevuelveEstadoCupoInscriptosYPrecioBase() throws Exception {
        when(courseService.getDetail(7L, IRIS)).thenReturn(new CourseDetailResponse(7L, "Curso de Java", "Desc",
                CourseStatus.PUBLISHED, Fixtures.START, Fixtures.END, new BigDecimal("100.00"), 5, 1, 4,
                new SpecialtyResponse(1L, "Programacion"),
                new CourseDetailResponse.CourseInstructor(2L, "Iris", "Ruiz", "iris@example.com")));

        mockMvc.perform(get(PATH + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.maxCapacity").value(5))
                .andExpect(jsonPath("$.enrolledStudents").value(1))
                .andExpect(jsonPath("$.basePrice").value(100.00));
    }

    @Test
    void getByIdDeCursoAjenoResponde403() throws Exception {
        when(courseService.getDetail(7L, IRIS)).thenThrow(new AuthorizationException(ErrorCode.AUTHORIZATION_COURSE_NOT_OWNER));

        mockMvc.perform(get(PATH + "/7")).andExpect(status().isForbidden());
    }

    @Test
    void getByIdInexistenteResponde404() throws Exception {
        when(courseService.getDetail(99L, IRIS)).thenThrow(new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND, 99L));

        mockMvc.perform(get(PATH + "/99")).andExpect(status().isNotFound());
    }

    @Test
    void createConLosCamposDelTp6Responde201() throws Exception {
        when(courseService.create(any(CourseRequest.class), eq(IRIS))).thenReturn(Fixtures.course(7L, CourseStatus.DRAFT));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/instructor/courses/7"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CourseRequest> request = ArgumentCaptor.forClass(CourseRequest.class);
        verify(courseService).create(request.capture(), eq(IRIS));
        assertThat(request.getValue().getSpecialtyId()).isEqualTo(1L);
    }

    @Test
    void createSinTituloResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"Curso de Java\"", "\"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
        verify(courseService, never()).create(any(), any());
    }

    @Test
    void createComoAdministradorResponde403() throws Exception {
        AuthTestSupport.asDefaultAdministrator(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(courseService, never()).create(any(), any());
    }

    @Test
    void updateConSoloElTituloLoPasaAlServicio() throws Exception {
        when(courseService.updatePartially(eq(7L), any(UpdateCourseRequest.class), eq(IRIS)))
                .thenReturn(Fixtures.course(7L, CourseStatus.DRAFT));

        mockMvc.perform(put(PATH + "/7").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Java 2\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateCourseRequest> changes = ArgumentCaptor.forClass(UpdateCourseRequest.class);
        verify(courseService).updatePartially(eq(7L), changes.capture(), eq(IRIS));
        assertThat(changes.getValue().getTitle()).isEqualTo("Java 2");
        assertThat(changes.getValue().getBasePrice()).isNull();
    }

    @Test
    void updateConTituloVacioResponde400() throws Exception {
        mockMvc.perform(put(PATH + "/7").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void updateDeCursoQueNoEstaEnBorradorResponde409() throws Exception {
        when(courseService.updatePartially(eq(7L), any(UpdateCourseRequest.class), eq(IRIS)))
                .thenThrow(new CourseStateException(ErrorCode.COURSE_NOT_DRAFT));

        mockMvc.perform(put(PATH + "/7").contentType(MediaType.APPLICATION_JSON).content("{\"maxCapacity\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteResponde204() throws Exception {
        mockMvc.perform(delete(PATH + "/7")).andExpect(status().isNoContent());

        verify(courseService).delete(7L, IRIS);
    }

    @Test
    void deleteDeCursoPublicadoResponde409() throws Exception {
        doThrow(new CourseStateException(ErrorCode.COURSE_NOT_DRAFT))
                .when(courseService).delete(7L, IRIS);

        mockMvc.perform(delete(PATH + "/7")).andExpect(status().isConflict());
    }

    @Test
    void getStudentsDevuelveLosInscriptosDeSuCurso() throws Exception {
        when(enrollmentService.getEnrolledStudents(7L, IRIS)).thenReturn(List.of(new EnrolledStudentResponse(11L, 1L,
                "Ana", "Diaz", "ana1@example.com", "30000001", LocalDateTime.of(2026, 5, 10, 0, 0),
                new BigDecimal("100.00"))));

        mockMvc.perform(get(PATH + "/7/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana"));
    }

    @Test
    void getStudentsDeCursoAjenoResponde403() throws Exception {
        when(enrollmentService.getEnrolledStudents(7L, IRIS))
                .thenThrow(new AuthorizationException(ErrorCode.AUTHORIZATION_COURSE_NOT_OWNER));

        mockMvc.perform(get(PATH + "/7/students")).andExpect(status().isForbidden());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
}
