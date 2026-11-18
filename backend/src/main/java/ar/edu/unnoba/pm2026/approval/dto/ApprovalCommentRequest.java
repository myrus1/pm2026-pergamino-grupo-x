package ar.edu.unnoba.pm2026.approval.dto;

import lombok.Getter;
import lombok.Setter;

// TP6: cuerpo opcional de POST /instructor/courses/{id}/approval-requests (ApprovalRequestDTO: instructorComment)
@Getter
@Setter
public class ApprovalCommentRequest {

    private String instructorComment;
}
