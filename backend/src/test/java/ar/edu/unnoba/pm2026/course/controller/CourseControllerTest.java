package ar.edu.unnoba.pm2026.course.controller;

import static org.hamcrest.Matchers.containsString;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Instructor;
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
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de CourseController (slice web, sin contexto completo ni BD)
@WebMvcTest(CourseController.class)
class CourseControllerTest {

    private static final String PATH = "/api/courses";
    private static final String BODY = """
            {"title":"Curso de Java","description":"Desc","basePrice":100.00,
             "startDate":"2026-06-01","endDate":"2026-06-20","maxCapacity":5,
             "specialtyId":1,"instructorId":2}""";

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

    private Specialty specialty() {
        Specialty specialty = new Specialty("Programacion");
        TestIds.setId(specialty, 1L);
        return specialty;
    }

    private Instructor instructor() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.setId(instructor, 2L);
        return instructor;
    }

    private Course course() {
        Course course = new Course("Curso de Java", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, specialty(), instructor());
        TestIds.setId(course, 5L);
        return course;
    }

    @Test
    void getAllDevuelveLosCursosConEspecialidadesEInstructoresPorId() throws Exception {
        when(courseService.getAll()).thenReturn(List.of(course()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].specialtyId").value(1))
                .andExpect(jsonPath("$[0].instructorId").value(2));
    }

    @Test
    void getAllConAlumnoDevuelveSusCursos() throws Exception {
        when(enrollmentService.getCoursesOfStudent(8L)).thenReturn(List.of(course()));

        mockMvc.perform(get(PATH).param("studentId", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5));
        verify(courseService, never()).getAll();
    }

    @Test
    void getAllConAlumnoInexistenteResponde404() throws Exception {
        when(enrollmentService.getCoursesOfStudent(99L))
                .thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(get(PATH).param("studentId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void getByIdDevuelveElCurso() throws Exception {
        when(courseService.getById(5L)).thenReturn(course());

        mockMvc.perform(get(PATH + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Curso de Java"))
                .andExpect(content().string(containsString("\"basePrice\":100.00")));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(courseService.getById(99L)).thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(get(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElCursoCreado() throws Exception {
        Course created = new Course("Curso de Java", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, specialty(), instructor());
        TestIds.setId(created, 7L);
        when(courseService.create(any(CourseRequest.class), any(AuthenticatedUser.class)))
                .thenReturn(created);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.specialtyId").value(1))
                .andExpect(jsonPath("$.instructorId").value(2));
    }

    @Test
    void createSinTituloResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Desc","basePrice":100.00,"startDate":"2026-06-01",
                 "endDate":"2026-06-20","maxCapacity":5,"specialtyId":1,"instructorId":2}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("title")));
    }

    @Test
    void createConEspecialidadInexistenteResponde404() throws Exception {
        when(courseService.create(any(CourseRequest.class), any(AuthenticatedUser.class)))
                .thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 1"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 1"));
    }

    @Test
    void updateModificaElCurso() throws Exception {
        Course updated = new Course("Curso de Java Avanzado", "Desc", new BigDecimal("100.00"),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20), 5, specialty(), instructor());
        TestIds.setId(updated, 5L);
        when(courseService.update(eq(5L), any(CourseRequest.class), any(AuthenticatedUser.class)))
                .thenReturn(updated);

        mockMvc.perform(put(PATH + "/5").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Curso de Java Avanzado","description":"Desc","basePrice":100.00,
                 "startDate":"2026-06-01","endDate":"2026-06-20","maxCapacity":5,
                 "specialtyId":1,"instructorId":2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Curso de Java Avanzado"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(courseService.update(eq(99L), any(CourseRequest.class), any(AuthenticatedUser.class)))
                .thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(put(PATH + "/99").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        mockMvc.perform(delete(PATH + "/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        doThrow(new EntityNotFoundException("Curso no encontrado con id 99"))
                .when(courseService).delete(99L, AuthTestSupport.ADMIN);

        mockMvc.perform(delete(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void getAllEsPublicoYSinTokenResponde200() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);
        when(courseService.getAll()).thenReturn(List.of(course()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getByIdEsPublicoYSinTokenResponde200() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);
        when(courseService.getById(5L)).thenReturn(course());

        mockMvc.perform(get(PATH + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getAllConAlumnoSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH).param("studentId", "8")).andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).getCoursesOfStudent(any());
    }

    @Test
    void getAllDeOtroAlumnoResponde403() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 9L);

        mockMvc.perform(get(PATH).param("studentId", "7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puede ver sus propios cursos"));
        verify(enrollmentService, never()).getCoursesOfStudent(any());
    }

    @Test
    void createSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(courseService, never()).create(any(CourseRequest.class), any(AuthenticatedUser.class));
    }

    @Test
    void createConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(courseService, never()).create(any(CourseRequest.class), any(AuthenticatedUser.class));
    }

    @Test
    void updateSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(put(PATH + "/5").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(courseService, never()).update(any(), any(CourseRequest.class), any(AuthenticatedUser.class));
    }

    @Test
    void updateDeCursoAjenoResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);
        when(courseService.update(eq(5L), any(CourseRequest.class), any(AuthenticatedUser.class)))
                .thenThrow(new AuthorizationException("El curso pertenece a otro instructor"));

        mockMvc.perform(put(PATH + "/5").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El curso pertenece a otro instructor"));
    }

    @Test
    void deleteSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(delete(PATH + "/5")).andExpect(status().isUnauthorized());
        verify(courseService, never()).delete(any(), any(AuthenticatedUser.class));
    }
}
