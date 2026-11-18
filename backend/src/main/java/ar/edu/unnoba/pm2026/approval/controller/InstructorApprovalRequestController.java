package ar.edu.unnoba.pm2026.approval.controller;

import ar.edu.unnoba.pm2026.approval.dto.ApprovalCommentRequest;
import ar.edu.unnoba.pm2026.approval.dto.ApprovalRequestResponse;
import ar.edu.unnoba.pm2026.approval.service.ApprovalRequestService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#41): el instructor pide la aprobacion de un curso suyo y consulta sus solicitudes.
@SecurityRequirement(name = "token")
@RestController
public class InstructorApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final AuthorizationService authorizationService;

    public InstructorApprovalRequestController(ApprovalRequestService approvalRequestService,
            AuthorizationService authorizationService) {
        this.approvalRequestService = approvalRequestService;
        this.authorizationService = authorizationService;
    }

    // El cuerpo es opcional. 403 si el curso es de otro; 409 si no esta en borrador.
    @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    @PostMapping("/instructor/courses/{id}/approval-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApprovalRequestResponse> submit(@PathVariable Long id,
            @RequestBody(required = false) ApprovalCommentRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        String comment = request == null ? null : request.getInstructorComment();
        ApprovalRequestResponse created = ApprovalRequestResponse.from(approvalRequestService.submit(id, comment,
                actor));
        // La solicitud se consulta en /api/approval-requests/{id} (el instructor ve las suyas)
        return ResponseEntity.created(ResourceLocation.forPath("/api/approval-requests/{id}", created.getId()))
                .body(created);
    }

    @GetMapping("/instructor/approval-requests")
    public List<ApprovalRequestResponse> getMine(
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return approvalRequestService.getByInstructor(actor.id()).stream()
                .map(ApprovalRequestResponse::from)
                .toList();
    }
}
