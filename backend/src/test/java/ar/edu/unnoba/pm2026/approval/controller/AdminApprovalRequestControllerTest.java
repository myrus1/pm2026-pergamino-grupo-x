package ar.edu.unnoba.pm2026.approval.controller;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import ar.edu.unnoba.pm2026.approval.service.ApprovalRequestService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
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

// Contrato HTTP de /admin/approval-requests (TP6, #39)
@WebMvcTest(AdminApprovalRequestController.class)
class AdminApprovalRequestControllerTest {

    private static final String PATH = "/admin/approval-requests";

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

    private ApprovalRequest pending(Long id) {
        ApprovalRequest request = new ApprovalRequest(Fixtures.course(3L, CourseStatus.DRAFT), "Listo");
        TestIds.setId(request, id);
        return request;
    }

    @Test
    void getDevuelveLasSolicitudesPendientes() throws Exception {
        when(approvalRequestService.getPending()).thenReturn(List.of(pending(1L)));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].courseId").value(3))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    }

    @Test
    void putConApprovedResuelveConElAdminDelToken() throws Exception {
        ApprovalRequest request = pending(1L);
        request.approve(Fixtures.administrator(3L), "Ok");
        when(approvalRequestService.resolve(1L, ApprovalStatus.APPROVED, 3L, "Ok")).thenReturn(request);

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"adminComment\":\"Ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.administratorComment").value("Ok"))
                .andExpect(jsonPath("$.administratorId").value(3));
    }

    @Test
    void putSinStatusResponde400() throws Exception {
        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"adminComment\":\"Ok\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        verify(approvalRequestService, never()).resolve(any(), any(), any(), any());
    }

    @Test
    void putConUnStatusQueNoExisteResponde400() throws Exception {
        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
        verify(approvalRequestService, never()).resolve(any(), any(), any(), any());
    }

    @Test
    void putConSubmittedResponde400() throws Exception {
        when(approvalRequestService.resolve(1L, ApprovalStatus.SUBMITTED, 3L, null)).thenThrow(new ValidationException(ErrorCode.APPROVAL_REQUEST_INVALID_DECISION));

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("approval-request.invalid-decision"));
    }

    @Test
    void putDeSolicitudInexistenteResponde404() throws Exception {
        when(approvalRequestService.resolve(99L, ApprovalStatus.REJECTED, 3L, null))
                .thenThrow(new ResourceNotFoundException(ErrorCode.APPROVAL_REQUEST_NOT_FOUND, 99L));

        mockMvc.perform(put(PATH + "/99").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putDeSolicitudYaResueltaResponde409() throws Exception {
        when(approvalRequestService.resolve(1L, ApprovalStatus.REJECTED, 3L, null))
                .thenThrow(new ApprovalRequestException(ErrorCode.APPROVAL_REQUEST_ALREADY_RESOLVED));

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void conTokenDeInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 4L);

        mockMvc.perform(get(PATH)).andExpect(status().isForbidden());
        verify(approvalRequestService, never()).getPending();
    }

    @Test
    void sinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(put(PATH + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
    }
}
