package ar.edu.unnoba.pm2026.approval.controller;

import ar.edu.unnoba.pm2026.approval.dto.ApprovalDecisionRequest;
import ar.edu.unnoba.pm2026.approval.dto.ApprovalRequestResponse;
import ar.edu.unnoba.pm2026.approval.service.ApprovalRequestService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#39): el administrador revisa las solicitudes pendientes y las resuelve con un PUT.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/admin/approval-requests")
public class AdminApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final AuthorizationService authorizationService;

    public AdminApprovalRequestController(ApprovalRequestService approvalRequestService,
            AuthorizationService authorizationService) {
        this.approvalRequestService = approvalRequestService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ApprovalRequestResponse> getPending(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return approvalRequestService.getPending().stream().map(ApprovalRequestResponse::from).toList();
    }

    // status APPROVED publica el curso; REJECTED lo vuelve a borrador. 409 si ya estaba resuelta.
    @PutMapping("/{id}")
    public ApprovalRequestResponse resolve(@PathVariable Long id, @RequestBody @Valid ApprovalDecisionRequest decision,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR);
        return ApprovalRequestResponse.from(approvalRequestService.resolve(id, decision.getStatus(), actor.id(),
                decision.getAdminComment()));
    }
}
