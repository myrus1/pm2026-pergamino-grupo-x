package com.example.demo.approval.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalResolveRequest {

    private Long administratorId;
    private String administratorComment;
}
