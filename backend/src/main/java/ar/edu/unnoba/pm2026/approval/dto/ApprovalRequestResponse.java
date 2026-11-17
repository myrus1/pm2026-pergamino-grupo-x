package ar.edu.unnoba.pm2026.approval.dto;

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
}
