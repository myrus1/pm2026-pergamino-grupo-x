package com.example.demo.approval.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalSubmitRequest {

    private Long courseId;
    private String instructorComment;
}
