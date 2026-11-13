package com.example.demo.user.controllers;

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

import com.example.demo.enrollment.services.EnrollmentService;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Student;
import com.example.demo.user.services.StudentService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.services.CourseService;
import com.example.demo.testutil.AutorizacionPrueba;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de StudentController (slice web, sin contexto completo ni BD)
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String RUTA = "/api/students";

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
    void autorizarPorDefecto() {
        AutorizacionPrueba.porDefecto(authorizationService);
    }

    private Student alumno() {
        Student alumno = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        TestIds.fijarId(alumno, 1L);
        return alumno;
    }

    @Test
    void getAllDevuelveLaListaDeAlumnosSinPassword() throws Exception {
        when(studentService.getAll()).thenReturn(List.of(alumno()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].dni").value("11111111"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getAllConCursoDevuelveLosAlumnosInscriptos() throws Exception {
        when(enrollmentService.getStudentsOfCourse(3L)).thenReturn(List.of(alumno()));

        mockMvc.perform(get(RUTA).param("courseId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value("11111111"));
        verify(studentService, never()).getAll();
    }

    @Test
    void getAllConCursoInexistenteResponde404() throws Exception {
        when(enrollmentService.getStudentsOfCourse(99L)).thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        mockMvc.perform(get(RUTA).param("courseId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado con id 99"));
    }

    @Test
    void getByIdDevuelveElAlumno() throws Exception {
        when(studentService.getById(1L)).thenReturn(alumno());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElAlumnoCreado() throws Exception {
        when(studentService.create(any(Student.class))).thenAnswer(invocation -> {
            Student guardado = invocation.getArgument(0);
            TestIds.fijarId(guardado, 7L);
            return guardado;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.dni").value("11111111"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElAlumnoExistente() throws Exception {
        when(studentService.getById(1L)).thenReturn(alumno());
        when(studentService.update(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana Maria","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ana Maria"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"11111111"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(studentService.getById(1L)).thenReturn(alumno());

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Estudiante no encontrado con id 99"));
    }

    @Test
    void getAllSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(studentService, never()).getAll();
    }

    @Test
    void getAllComoAlumnoResponde403() throws Exception {
        AutorizacionPrueba.sinPermiso(authorizationService);

        mockMvc.perform(get(RUTA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(studentService, never()).getAll();
    }

    @Test
    void getAllDelCursoSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(get(RUTA).param("courseId", "3"))
                .andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).getStudentsOfCourse(any());
    }

    @Test
    void getAllDelCursoDeOtroInstructorResponde403() throws Exception {
        AutorizacionPrueba.comoInstructor(authorizationService, 4L);
        when(courseService.getById(3L, new UsuarioAutenticado(4L, "iris@example.com", Rol.INSTRUCTOR)))
                .thenThrow(new AuthorizationException("El curso pertenece a otro instructor"));

        mockMvc.perform(get(RUTA).param("courseId", "3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El curso pertenece a otro instructor"));
        verify(enrollmentService, never()).getStudentsOfCourse(any());
    }

    @Test
    void createEsPublicoYSinTokenResponde201() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);
        when(studentService.create(any(Student.class))).thenAnswer(invocation -> {
            Student creado = invocation.getArgument(0);
            TestIds.fijarId(creado, 7L);
            return creado;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"44555666"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void updateSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"44555666"}"""))
                .andExpect(status().isUnauthorized());
        verify(studentService, never()).update(any(Student.class));
    }

    @Test
    void updateConRolNoPermitidoResponde403() throws Exception {
        AutorizacionPrueba.sinPermiso(authorizationService);

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perez","email":"ana@example.com","password":"secret","dni":"44555666"}"""))
                .andExpect(status().isForbidden());
        verify(studentService, never()).update(any(Student.class));
    }

    @Test
    void deleteSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(delete(RUTA + "/1")).andExpect(status().isUnauthorized());
        verify(studentService, never()).delete(any());
    }

    @Test
    void getByIdSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(get(RUTA + "/1")).andExpect(status().isUnauthorized());
        verify(studentService, never()).getById(any());
    }
}
