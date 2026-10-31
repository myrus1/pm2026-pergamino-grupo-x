package com.example.demo.approval.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.approval.dto.ApprovalRequestResponse;
import com.example.demo.approval.dto.ApprovalResolveRequest;
import com.example.demo.approval.dto.ApprovalSubmitRequest;
import com.example.demo.approval.model.ApprovalRequest;
import com.example.demo.approval.model.ApprovalStatus;
import com.example.demo.approval.services.ApprovalRequestService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

    public ApprovalRequestController(ApprovalRequestService approvalRequestService) {
        this.approvalRequestService = approvalRequestService;
    }

    @GetMapping
    public List<ApprovalRequestResponse> getAll(@RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) Long instructorId) {
        List<ApprovalRequest> solicitudes = instructorId == null
                ? approvalRequestService.getAll()
                : approvalRequestService.getByInstructor(instructorId);
        return solicitudes.stream()
                .filter(solicitud -> status == null || solicitud.getStatus() == status)
                .map(this::respuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public ApprovalRequestResponse getById(@PathVariable Long id) {
        return respuesta(approvalRequestService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalRequestResponse submit(@RequestBody ApprovalSubmitRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getCourseId(), "courseId");
        return respuesta(approvalRequestService.submit(solicitud.getCourseId(), solicitud.getInstructorComment()));
    }

    @PostMapping("/{id}/approve")
    public ApprovalRequestResponse approve(@PathVariable Long id, @RequestBody ApprovalResolveRequest resolucion) {
        validar(resolucion);
        return respuesta(approvalRequestService.approve(id, resolucion.getAdministratorId(),
                resolucion.getAdministratorComment()));
    }

    @PostMapping("/{id}/reject")
    public ApprovalRequestResponse reject(@PathVariable Long id, @RequestBody ApprovalResolveRequest resolucion) {
        validar(resolucion);
        return respuesta(approvalRequestService.reject(id, resolucion.getAdministratorId(),
                resolucion.getAdministratorComment()));
    }

    private void validar(ApprovalResolveRequest resolucion) {
        if (resolucion == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(resolucion.getAdministratorId(), "administratorId");
    }

    private ApprovalRequestResponse respuesta(ApprovalRequest solicitud) {
        return new ApprovalRequestResponse(solicitud.getId(), solicitud.getCourse().getId(), solicitud.getStatus(),
                solicitud.getRequestDate(), solicitud.getResolutionDate(), solicitud.getInstructorComment(),
                solicitud.getAdministratorComment(),
                solicitud.getAdministrator() == null ? null : solicitud.getAdministrator().getId());
    }
}
