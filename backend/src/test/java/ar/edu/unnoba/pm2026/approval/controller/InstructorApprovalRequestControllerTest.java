package ar.edu.unnoba.pm2026.approval.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.service.ApprovalRequestService;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de las solicitudes de aprobacion del instructor (TP6, #41)
@WebMvcTest(InstructorApprovalRequestController.class)
class InstructorApprovalRequestControllerTest {

    private static final AuthenticatedUser IRIS = new AuthenticatedUser(2L, "iris@example.com", Role.INSTRUCTOR);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApprovalRequestService approvalRequestService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.as(authorizationService, IRIS);
    }

    private ApprovalRequest submitted(String comment) {
        ApprovalRequest request = new ApprovalRequest(Fixtures.course(7L, CourseStatus.DRAFT), comment);
        TestIds.setId(request, 12L);
        return request;
    }

    @Test
    void submitConComentarioResponde201() throws Exception {
        when(approvalRequestService.submit(7L, "Listo", IRIS)).thenReturn(submitted("Listo"));

        mockMvc.perform(post("/instructor/courses/7/approval-requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructorComment\":\"Listo\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/approval-requests/12"))
                .andExpect(jsonPath("$.courseId").value(7))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.instructorComment").value("Listo"));
    }

    @Test
    void submitSinCuerpoTambienFunciona() throws Exception {
        when(approvalRequestService.submit(7L, null, IRIS)).thenReturn(submitted(null));

        mockMvc.perform(post("/instructor/courses/7/approval-requests"))
                .andExpect(status().isCreated());
        verify(approvalRequestService).submit(7L, null, IRIS);
    }

    @Test
    void submitDeCursoQueNoEstaEnBorradorResponde409() throws Exception {
        when(approvalRequestService.submit(7L, null, IRIS)).thenThrow(
                new ApprovalRequestException("Solo se puede solicitar la aprobacion de un curso en borrador"));

        mockMvc.perform(post("/instructor/courses/7/approval-requests")).andExpect(status().isConflict());
    }

    @Test
    void submitDeCursoAjenoResponde403() throws Exception {
        when(approvalRequestService.submit(7L, null, IRIS))
                .thenThrow(new AuthorizationException("El curso pertenece a otro instructor"));

        mockMvc.perform(post("/instructor/courses/7/approval-requests")).andExpect(status().isForbidden());
    }

    @Test
    void submitComoAdministradorResponde403() throws Exception {
        AuthTestSupport.asDefaultAdministrator(authorizationService);

        mockMvc.perform(post("/instructor/courses/7/approval-requests")).andExpect(status().isForbidden());
        verify(approvalRequestService, never()).submit(any(), isNull(), any());
    }

    @Test
    void getDevuelveSusSolicitudes() throws Exception {
        when(approvalRequestService.getByInstructor(2L)).thenReturn(List.of(submitted("Listo")));

        mockMvc.perform(get("/instructor/approval-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12));
    }
}
