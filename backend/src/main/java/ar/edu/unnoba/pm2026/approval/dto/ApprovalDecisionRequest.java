package ar.edu.unnoba.pm2026.approval.dto;

import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// TP6: PUT /admin/approval-requests/{id} (ResolveApprovalRequestDTO del enunciado: status APPROVED/REJECTED, adminComment).
// El administrador que resuelve sale del token.
@Getter
@Setter
public class ApprovalDecisionRequest {

    @NotNull
    private ApprovalStatus status;
    private String adminComment;
}
