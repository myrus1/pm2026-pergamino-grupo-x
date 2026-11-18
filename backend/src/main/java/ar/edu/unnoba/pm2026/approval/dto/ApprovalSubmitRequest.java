package ar.edu.unnoba.pm2026.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalSubmitRequest {

    @NotNull
    private Long courseId;
    private String instructorComment;
}
