package ar.edu.unnoba.pm2026.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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

import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de StudentController (slice web, sin contexto completo ni BD)
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String PATH = "/api/students";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private EnrollmentService enrollmentService;
    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private CourseService courseService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    private Student student() {
        Student student = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        TestIds.setId(student, 1L);
        return student;
    }

    @Test
    void getAllDevuelveLaListaDeAlumnosSinPassword() throws Exception {
        when(studentService.getAll()).thenReturn(List.of(student()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].dni").value("11111111"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getAllConCursoDevuelveLosAlumnosInscriptos() throws Exception {
        when(enrollmentService.getStudentsOfCourse(3L)).thenReturn(List.of(student()));

        mockMvc.perform(get(PATH).param("courseId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value("11111111"));
        verify(studentService, never()).getAll();
    }

    @Test
    void getAllConCursoInexistenteResponde404() throws Exception {
        when(enrollmentService.getStudentsOfCourse(99L)).thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(get(PATH).param("courseId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void getByIdDevuelveElAlumno() throws Exception {
        when(studentService.getById(1L)).thenReturn(student());

        mockMvc.perform(get(PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(get(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElAlumnoCreado() throws Exception {
        when(studentService.create(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            TestIds.setId(saved, 7L);
            return saved;
        });

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.dni").value("11111111"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElAlumnoExistente() throws Exception {
        when(studentService.getById(1L)).thenReturn(student());
        when(studentService.update(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana Maria","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ana Maria"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(put(PATH + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(studentService.getById(1L)).thenReturn(student());

        mockMvc.perform(delete(PATH + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(delete(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void getAllSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(studentService, never()).getAll();
    }

    @Test
    void getAllComoAlumnoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(studentService, never()).getAll();
    }

    @Test
    void getAllDelCursoSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH).param("courseId", "3"))
                .andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).getStudentsOfCourse(any());
    }

    @Test
    void getAllDelCursoDeOtroInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);
        when(courseService.getById(3L, new AuthenticatedUser(4L, "iris@example.com", Role.INSTRUCTOR)))
                .thenThrow(new AuthorizationException("El curso pertenece a otro instructor"));

        mockMvc.perform(get(PATH).param("courseId", "3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El curso pertenece a otro instructor"));
        verify(enrollmentService, never()).getStudentsOfCourse(any());
    }

    @Test
    void createEsPublicoYSinTokenResponde201() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);
        when(studentService.create(any(Student.class))).thenAnswer(invocation -> {
            Student created = invocation.getArgument(0);
            TestIds.setId(created, 7L);
            return created;
        });

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"44555666"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void updateSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"44555666"}"""))
                .andExpect(status().isUnauthorized());
        verify(studentService, never()).update(any(Student.class));
    }

    @Test
    void updateConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"44555666"}"""))
                .andExpect(status().isForbidden());
        verify(studentService, never()).update(any(Student.class));
    }

    @Test
    void deleteSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(delete(PATH + "/1")).andExpect(status().isUnauthorized());
        verify(studentService, never()).delete(any());
    }

    @Test
    void getByIdSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH + "/1")).andExpect(status().isUnauthorized());
        verify(studentService, never()).getById(any());
    }
}
