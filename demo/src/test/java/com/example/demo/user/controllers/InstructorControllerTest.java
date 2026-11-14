package com.example.demo.user.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.course.dto.SpecialtyResponse;
import com.example.demo.enrollment.services.EnrollmentService;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.never;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.testutil.AutorizacionPrueba;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de InstructorController (slice web, sin contexto completo ni BD)
@WebMvcTest(InstructorController.class)
class InstructorControllerTest {

    private static final String RUTA = "/api/instructors";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    @MockitoBean
    private EnrollmentService enrollmentService;
    private static final String CUERPO = """
            {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}""";

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void autorizarPorDefecto() {
        AutorizacionPrueba.porDefecto(authorizationService);
    }

    private Instructor instructor() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.fijarId(instructor, 1L);
        return instructor;
    }

    @Test
    void getAllDevuelveLaListaDeInstructoresSinPassword() throws Exception {
        when(instructorService.getAll()).thenReturn(List.of(instructor()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("iris@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getByIdDevuelveElInstructor() throws Exception {
        when(instructorService.getById(1L)).thenReturn(instructor());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Iris"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void createValidoResponde201ConElInstructorCreado() throws Exception {
        when(instructorService.create(any(Instructor.class))).thenAnswer(invocation -> {
            Instructor guardado = invocation.getArgument(0);
            TestIds.fijarId(guardado, 7L);
            return guardado;
        });

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createConEmailExistenteResponde409() throws Exception {
        when(instructorService.create(any(Instructor.class)))
                .thenThrow(new InstructorEmailAlreadyExistsException("iris@example.com"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un instructor con el email iris@example.com"));
    }

    @Test
    void createSinCamposObligatoriosResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("firstName")));
    }

    @Test
    void updateModificaElInstructorExistente() throws Exception {
        when(instructorService.getById(1L)).thenReturn(instructor());
        when(instructorService.update(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris Maria","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Iris Maria"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(instructorService.getById(1L)).thenReturn(instructor());

        mockMvc.perform(delete(RUTA + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(delete(RUTA + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void getSpecialtiesDevuelveLasEspecialidadesDelInstructor() throws Exception {
        when(instructorService.getSpecialties(1L)).thenReturn(List.of(
                new SpecialtyResponse(4L, "Programacion"),
                new SpecialtyResponse(5L, "Diseno")));

        mockMvc.perform(get(RUTA + "/1/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].name").value("Programacion"))
                .andExpect(jsonPath("$[1].id").value(5))
                .andExpect(jsonPath("$[1].name").value("Diseno"));
    }

    @Test
    void getSpecialtiesSinEspecialidadesDevuelveListaVacia() throws Exception {
        when(instructorService.getSpecialties(1L)).thenReturn(List.of());

        mockMvc.perform(get(RUTA + "/1/specialties"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getSpecialtiesDeInstructorInexistenteResponde404() throws Exception {
        when(instructorService.getSpecialties(99L))
                .thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99/specialties"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void revenueDevuelveLoRecaudadoPorElInstructor() throws Exception {
        when(enrollmentService.instructorRevenue(1L)).thenReturn(new BigDecimal("117.00"));

        mockMvc.perform(get(RUTA + "/1/revenue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\":117.00")));
    }

    @Test
    void revenueDeInstructorInexistenteResponde404() throws Exception {
        when(enrollmentService.instructorRevenue(99L))
                .thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(get(RUTA + "/99/revenue"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void asignarEspecialidadDelegaEnElServicioYResponde204() throws Exception {
        mockMvc.perform(put(RUTA + "/1/specialties/2"))
                .andExpect(status().isNoContent());

        verify(instructorService).addSpecialty(1L, 2L);
    }

    @Test
    void asignarEspecialidadInexistenteResponde404() throws Exception {
        doThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"))
                .when(instructorService).addSpecialty(1L, 99L);

        mockMvc.perform(put(RUTA + "/1/specialties/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void quitarEspecialidadDelegaEnElServicioYResponde204() throws Exception {
        mockMvc.perform(delete(RUTA + "/1/specialties/2"))
                .andExpect(status().isNoContent());

        verify(instructorService).removeSpecialty(1L, 2L);
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Iris","lastName":"Ruiz","email":"iris@example.com","password":"secret"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Instructor no encontrado con id 99"));
    }

    @Test
    void getAllEsPublicoYSinTokenResponde200() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);
        when(instructorService.getAll()).thenReturn(List.of(instructor()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getByIdEsPublicoYSinTokenResponde200() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);
        when(instructorService.getById(1L)).thenReturn(instructor());

        mockMvc.perform(get(RUTA + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void revenueSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(get(RUTA + "/4/revenue")).andExpect(status().isUnauthorized());
        verify(enrollmentService, never()).instructorRevenue(any());
    }

    @Test
    void revenueDeOtroInstructorResponde403() throws Exception {
        AutorizacionPrueba.comoInstructor(authorizationService, 4L);

        mockMvc.perform(get(RUTA + "/99/revenue"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("La recaudacion no le pertenece"));
        verify(enrollmentService, never()).instructorRevenue(any());
    }

    @Test
    void revenueDelInstructorPropioResponde200() throws Exception {
        AutorizacionPrueba.comoInstructor(authorizationService, 4L);
        when(enrollmentService.instructorRevenue(4L)).thenReturn(new BigDecimal("70.00"));

        mockMvc.perform(get(RUTA + "/4/revenue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"total\":70.00")));
    }

    @Test
    void createSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        verify(instructorService, never()).create(any());
    }

    @Test
    void createConRolNoPermitidoResponde403() throws Exception {
        AutorizacionPrueba.sinPermiso(authorizationService);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());
        verify(instructorService, never()).create(any());
    }

    @Test
    void updateSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(put(RUTA + "/4").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        verify(instructorService, never()).update(any());
    }

    @Test
    void updateDeOtroInstructorResponde403() throws Exception {
        AutorizacionPrueba.comoInstructor(authorizationService, 4L);

        mockMvc.perform(put(RUTA + "/99").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puede modificar su propio perfil"));
        verify(instructorService, never()).update(any());
    }

    @Test
    void deleteSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(delete(RUTA + "/4")).andExpect(status().isUnauthorized());
        verify(instructorService, never()).delete(any());
    }

    @Test
    void asignarEspecialidadSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(put(RUTA + "/4/specialties/1")).andExpect(status().isUnauthorized());
        verify(instructorService, never()).addSpecialty(any(), any());
    }

    @Test
    void quitarEspecialidadSinTokenResponde401() throws Exception {
        AutorizacionPrueba.sinToken(authorizationService);

        mockMvc.perform(delete(RUTA + "/4/specialties/1")).andExpect(status().isUnauthorized());
        verify(instructorService, never()).removeSpecialty(any(), any());
    }
}
