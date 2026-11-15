package ar.edu.unnoba.pm2026.enrollment.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de EnrollmentController (slice web, sin contexto completo ni BD)
@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    private static final String PATH = "/api/enrollments";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;
    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    private Student student() {
        Student student = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        TestIds.setId(student, 1L);
        return student;
    }

    // Curso publicado que comienza en el futuro (requisito de Student.applyCourse)
    private Course publishedCourse() {
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.now().plusWeeks(2),
                LocalDate.now().plusWeeks(6), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        course.setStatus(CourseStatus.PUBLISHED);
        TestIds.setId(course, 2L);
        return course;
    }

    private Enrollment enrollment() {
        Enrollment enrollment = new Enrollment(student(), publishedCourse(), new BigDecimal("100.00"));
        TestIds.setId(enrollment, 9L);
        return enrollment;
    }

    @Test
    void getAllDevuelveLaListaDeInscripcionesConLosIdsRelacionados() throws Exception {
        when(enrollmentService.getAll()).thenReturn(List.of(enrollment()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].courseId").value(2));
    }

    @Test
    void getByIdDevuelveLaInscripcion() throws Exception {
        when(enrollmentService.getById(9L, AuthTestSupport.ADMIN)).thenReturn(enrollment());

        mockMvc.perform(get(PATH + "/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(enrollmentService.getById(99L, AuthTestSupport.ADMIN)).thenThrow(new EntityNotFoundException("Inscripcion no encontrada con id 99"));

        mockMvc.perform(get(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Inscripcion no encontrada con id 99"));
    }

    @Test
    void createValidoResponde201ConLaInscripcionGenerada() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenReturn(enrollment());

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
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

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":1,"courseId":2}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El curso no esta publicado"));
    }

    @Test
    void createConAlumnoInexistenteResponde404() throws Exception {
        when(enrollmentService.enroll(99L, 2L))
                .thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":99,"courseId":2}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void createSinIdsResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("studentId")));
    }

    @Test
    void revenueDevuelveElTotalRecaudado() throws Exception {
        when(enrollmentService.totalRevenue()).thenReturn(new BigDecimal("160.00"));

        mockMvc.perform(get(PATH + "/revenue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\":160.00")));
    }

    @Test
    void updateNoEstaPermitidoResponde405() throws Exception {
        mockMvc.perform(put(PATH + "/9").contentType(MediaType.APPLICATION_JSON).content("""
                {"studentId":1,"courseId":2}"""))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void cancelarUnaInscripcionNoEstaPermitidoResponde405() throws Exception {
        mockMvc.perform(delete(PATH + "/9"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void getAllSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(enrollmentService, never()).getAll();
    }

    @Test
    void getAllComoAlumnoDevuelveSoloSusInscripciones() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 1L);
        when(enrollmentService.getAllByStudent(1L)).thenReturn(List.of(enrollment()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1));
        verify(enrollmentService, never()).getAll();
    }

    @Test
    void getAllComoInstructorDevuelveSoloLasDeSusCursos() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);
        when(enrollmentService.getAllByInstructor(4L)).thenReturn(List.of(enrollment()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1));
        verify(enrollmentService, never()).getAll();
    }

    @Test
    void revenueSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH + "/revenue")).andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).totalRevenue();
    }

    @Test
    void getByIdSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH + "/9")).andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).getById(any(Long.class));
    }

    @Test
    void getByIdAjenoResponde403() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 9L);
        when(enrollmentService.getById(9L, new AuthenticatedUser(9L, "ana@example.com", Role.STUDENT)))
                .thenThrow(new AuthorizationException("La inscripcion no le pertenece"));

        mockMvc.perform(get(PATH + "/9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("La inscripcion no le pertenece"));
    }

    @Test
    void createSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":1,\"courseId\":2}"))
                .andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).enroll(any(), any());
    }

    @Test
    void createConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":1,\"courseId\":2}"))
                .andExpect(status().isForbidden());
        verify(enrollmentService, never()).enroll(any(), any());
    }

    @Test
    void createDeOtroAlumnoResponde403() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 9L);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":7,\"courseId\":2}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puede inscribirse a si mismo"));
        verify(enrollmentService, never()).enroll(any(), any());
    }

    @Test
    void createDeSiMismoResponde201() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 9L);
        when(enrollmentService.enroll(9L, 2L)).thenReturn(enrollment());

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":9,\"courseId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
        verify(enrollmentService).enroll(9L, 2L);
    }
}
