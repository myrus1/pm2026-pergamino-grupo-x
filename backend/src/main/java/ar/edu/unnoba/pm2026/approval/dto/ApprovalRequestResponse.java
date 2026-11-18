package ar.edu.unnoba.pm2026.approval.dto;

import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApprovalRequestResponse {

    private Long id;
    private Long courseId;
    private ApprovalStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime resolutionDate;
    private String instructorComment;
    private String administratorComment;
    private Long administratorId;

    // Solo lee los ids del curso y del admin, asi que se puede armar fuera de una transaccion
    public static ApprovalRequestResponse from(ApprovalRequest request) {
        return new ApprovalRequestResponse(request.getId(), request.getCourse().getId(), request.getStatus(),
                request.getRequestDate(), request.getResolutionDate(), request.getInstructorComment(),
                request.getAdministratorComment(),
                request.getAdministrator() == null ? null : request.getAdministrator().getId());
    }
}
