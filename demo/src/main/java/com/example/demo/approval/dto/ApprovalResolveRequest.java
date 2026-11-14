package com.example.demo.approval.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalResolveRequest {

    // El administrador que resuelve sale del token, no del body.
    private String administratorComment;
}
