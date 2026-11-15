package ar.edu.unnoba.pm2026.course.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.SpecialtyService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import jakarta.persistence.EntityNotFoundException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;

// Contrato HTTP de SpecialtyController (slice web, sin contexto completo ni BD)
@WebMvcTest(SpecialtyController.class)
class SpecialtyControllerTest {

    private static final String PATH = "/api/specialties";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecialtyService specialtyService;
    private static final String BODY = """
            {"name":"Programacion"}""";

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

    @Test
    void getAllDevuelveLaListaDeEspecialidades() throws Exception {
        when(specialtyService.getAll()).thenReturn(List.of(specialty()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Programacion"));
    }

    @Test
    void getByIdDevuelveLaEspecialidad() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(specialty());

        mockMvc.perform(get(PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Programacion"));
    }

    @Test
    void getByIdInexistenteResponde404ConMensaje() throws Exception {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        mockMvc.perform(get(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void createValidoResponde201ConLaEspecialidadCreada() throws Exception {
        when(specialtyService.save(any(Specialty.class))).thenAnswer(invocation -> {
            Specialty saved = invocation.getArgument(0);
            TestIds.setId(saved, 7L);
            return saved;
        });

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Bases de datos"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Bases de datos"));
    }

    private DataIntegrityViolationException databaseError(String sqlState) {
        return new DataIntegrityViolationException("error", new SQLException("error", sqlState));
    }

    @Test
    void createConNombreRepetidoResponde409() throws Exception {
        when(specialtyService.save(any(Specialty.class))).thenThrow(databaseError("23505"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Programacion"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Los datos ya existen o estan en uso por otro registro"));
    }

    @Test
    void deleteDeUnaEspecialidadEnUsoResponde409() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(specialty());
        doThrow(databaseError("23503")).when(specialtyService).delete(1L);

        mockMvc.perform(delete(PATH + "/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void createConUnNombreDemasiadoLargoResponde400() throws Exception {
        when(specialtyService.save(any(Specialty.class))).thenThrow(databaseError("22001"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Programacion"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos invalidos"));
    }

    @Test
    void createSinNombreResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("name")));
    }

    @Test
    void updateCambiaElNombreDeLaEspecialidad() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(specialty());
        when(specialtyService.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Algoritmos"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Algoritmos"));
    }

    @Test
    void updateInexistenteResponde404() throws Exception {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        mockMvc.perform(put(PATH + "/99").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Algoritmos"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void deleteResponde204() throws Exception {
        when(specialtyService.getById(1L)).thenReturn(specialty());

        mockMvc.perform(delete(PATH + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteResponde404() throws Exception {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        mockMvc.perform(delete(PATH + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Especialidad no encontrada con id 99"));
    }

    @Test
    void getAllEsPublicoYSinTokenResponde200() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);
        when(specialtyService.getAll()).thenReturn(List.of(specialty()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Programacion"));
    }

    @Test
    void createSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(specialtyService, never()).save(any());
    }

    @Test
    void createConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(specialtyService, never()).save(any());
    }

    @Test
    void updateSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(specialtyService, never()).getById(any());
    }

    @Test
    void deleteSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(delete(PATH + "/1")).andExpect(status().isUnauthorized());
        verify(specialtyService, never()).delete(any());
    }
}
