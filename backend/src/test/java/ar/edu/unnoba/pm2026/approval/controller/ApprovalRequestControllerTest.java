package ar.edu.unnoba.pm2026.approval.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.approval.service.ApprovalRequestService;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Administrator;
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
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;

@WebMvcTest(ApprovalRequestController.class)
class ApprovalRequestControllerTest {

    private static final String PATH = "/api/approval-requests";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApprovalRequestService approvalRequestService;
    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    private ApprovalRequest request(Long id) {
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        TestIds.setId(course, 3L);
        ApprovalRequest request = new ApprovalRequest(course, "Listo");
        TestIds.setId(request, id);
        return request;
    }

    private Administrator admin() {
        Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "secret");
        TestIds.setId(admin, 9L);
        return admin;
    }

    @Test
    void getAllFiltraPorEstado() throws Exception {
        ApprovalRequest pendiente = request(1L);
        ApprovalRequest approved = request(2L);
        approved.approve(admin(), null);
        when(approvalRequestService.getAll()).thenReturn(List.of(pendiente, approved));

        mockMvc.perform(get(PATH).param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].courseId").value(3));
    }

    @Test
    void getAllPorInstructorDevuelveSusSolicitudes() throws Exception {
        when(approvalRequestService.getByInstructor(4L)).thenReturn(List.of(request(1L)));

        mockMvc.perform(get(PATH).param("instructorId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].instructorComment").value("Listo"));
        verify(approvalRequestService, never()).getAll();
    }

    @Test
    void submitResponde201ConLaSolicitud() throws Exception {
        when(approvalRequestService.submit(3L, "Listo", AuthTestSupport.ADMIN)).thenReturn(request(1L));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"courseId":3,"instructorComment":"Listo"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.administratorId").doesNotExist());
    }

    @Test
    void submitSinCursoResponde400() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("courseId")));
    }

    @Test
    void submitDeCursoQueNoEstaEnBorradorResponde409() throws Exception {
        when(approvalRequestService.submit(3L, null, AuthTestSupport.ADMIN)).thenThrow(
                new ApprovalRequestException("Solo se puede solicitar la aprobacion de un curso en borrador"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                {"courseId":3}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Solo se puede solicitar la aprobacion de un curso en borrador"));
    }

    @Test
    void approveResponde200ConLaResolucion() throws Exception {
        ApprovalRequest approved = request(1L);
        approved.approve(admin(), "Ok");
        when(approvalRequestService.approve(1L, AuthTestSupport.ADMIN.id(), "Ok")).thenReturn(approved);

        mockMvc.perform(post(PATH + "/1/approve").contentType(MediaType.APPLICATION_JSON).content("""
                {"administratorComment":"Ok"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.administratorId").value(9))
                .andExpect(jsonPath("$.resolutionDate").exists());
    }

    @Test
    void rejectDeSolicitudYaResueltaResponde409() throws Exception {
        when(approvalRequestService.reject(1L, AuthTestSupport.ADMIN.id(), null))
                .thenThrow(new ApprovalRequestException("La solicitud ya fue resuelta"));

        mockMvc.perform(post(PATH + "/1/reject").contentType(MediaType.APPLICATION_JSON).content("""
                {}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La solicitud ya fue resuelta"));
    }

    @Test
    void rejectDeSolicitudInexistenteResponde404() throws Exception {
        when(approvalRequestService.reject(99L, AuthTestSupport.ADMIN.id(), null))
                .thenThrow(new EntityNotFoundException("Solicitud de aprobacion no encontrada con id 99"));

        mockMvc.perform(post(PATH + "/99/reject").contentType(MediaType.APPLICATION_JSON).content("""
                {}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Solicitud de aprobacion no encontrada con id 99"));
    }

    @Test
    void getAllSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token invalido o vencido"));
        verify(approvalRequestService, never()).getAll();
    }

    @Test
    void getAllConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(approvalRequestService, never()).getAll();
    }

    @Test
    void getAllComoInstructorUsaSoloLasSuyasAunquePidaOtra() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);
        when(approvalRequestService.getByInstructor(4L)).thenReturn(List.of(request(1L)));

        mockMvc.perform(get(PATH).param("instructorId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        verify(approvalRequestService).getByInstructor(4L);
        verify(approvalRequestService, never()).getAll();
    }

    @Test
    void getByIdSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get(PATH + "/1")).andExpect(status().isUnauthorized());
        verify(approvalRequestService, never()).getById(any(Long.class));
    }

    @Test
    void getByIdDeOtroInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);
        when(approvalRequestService.getById(1L, new AuthenticatedUser(4L, "iris@example.com", Role.INSTRUCTOR)))
                .thenThrow(new AuthorizationException("La solicitud de aprobacion no le pertenece"));

        mockMvc.perform(get(PATH + "/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("La solicitud de aprobacion no le pertenece"));
    }

    @Test
    void submitSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":3,\"instructorComment\":\"Listo\"}"))
                .andExpect(status().isUnauthorized());
        verify(approvalRequestService, never()).submit(any(), any(), any());
    }

    @Test
    void submitDeOtroInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);
        when(approvalRequestService.submit(3L, "Listo",
                new AuthenticatedUser(4L, "iris@example.com", Role.INSTRUCTOR)))
                .thenThrow(new AuthorizationException("El curso pertenece a otro instructor"));

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":3,\"instructorComment\":\"Listo\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El curso pertenece a otro instructor"));
    }

    @Test
    void approveSinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(post(PATH + "/1/approve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"administratorComment\":\"Ok\"}"))
                .andExpect(status().isUnauthorized());
        verify(approvalRequestService, never()).approve(any(), any(), any());
    }

    @Test
    void approveConRolNoPermitidoResponde403() throws Exception {
        AuthTestSupport.withoutPermission(authorizationService);

        mockMvc.perform(post(PATH + "/1/approve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"administratorComment\":\"Ok\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para esta operacion"));
        verify(approvalRequestService, never()).approve(any(), any(), any());
    }
}
