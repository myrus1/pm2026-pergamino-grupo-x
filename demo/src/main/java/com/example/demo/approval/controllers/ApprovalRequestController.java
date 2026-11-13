package com.example.demo.approval.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.approval.dto.ApprovalRequestResponse;
import com.example.demo.approval.dto.ApprovalResolveRequest;
import com.example.demo.approval.dto.ApprovalSubmitRequest;
import com.example.demo.approval.model.ApprovalRequest;
import com.example.demo.approval.model.ApprovalStatus;
import com.example.demo.approval.services.ApprovalRequestService;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final AuthorizationService authorizationService;

    public ApprovalRequestController(ApprovalRequestService approvalRequestService,
            AuthorizationService authorizationService) {
        this.approvalRequestService = approvalRequestService;
        this.authorizationService = authorizationService;
    }

    // El instructor solo ve las suyas; el administrador puede filtrar por cualquier instructor.
    @GetMapping
    public List<ApprovalRequestResponse> getAll(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) Long instructorId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ADMINISTRADOR, Rol.INSTRUCTOR);
        List<ApprovalRequest> solicitudes;
        if (actor.rol() == Rol.INSTRUCTOR) {
            solicitudes = approvalRequestService.getByInstructor(actor.id());
        } else {
            solicitudes = instructorId == null
                    ? approvalRequestService.getAll()
                    : approvalRequestService.getByInstructor(instructorId);
        }
        return solicitudes.stream()
                .filter(solicitud -> status == null || solicitud.getStatus() == status)
                .map(this::respuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public ApprovalRequestResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ADMINISTRADOR, Rol.INSTRUCTOR);
        return respuesta(approvalRequestService.getById(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalRequestResponse submit(@RequestBody ApprovalSubmitRequest solicitud,
            @RequestHeader(name = "Authorization", required = false) String token) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getCourseId(), "courseId");
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.INSTRUCTOR, Rol.ADMINISTRADOR);
        return respuesta(approvalRequestService.submit(solicitud.getCourseId(), solicitud.getInstructorComment(),
                actor));
    }

    @PostMapping("/{id}/approve")
    public ApprovalRequestResponse approve(@PathVariable Long id, @RequestBody ApprovalResolveRequest resolucion,
            @RequestHeader(name = "Authorization", required = false) String token) {
        validar(resolucion);
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        return respuesta(approvalRequestService.approve(id, actor.id(), resolucion.getAdministratorComment()));
    }

    @PostMapping("/{id}/reject")
    public ApprovalRequestResponse reject(@PathVariable Long id, @RequestBody ApprovalResolveRequest resolucion,
            @RequestHeader(name = "Authorization", required = false) String token) {
        validar(resolucion);
        UsuarioAutenticado actor = authorizationService.autorizar(token, Rol.ADMINISTRADOR);
        return respuesta(approvalRequestService.reject(id, actor.id(), resolucion.getAdministratorComment()));
    }

    private void validar(ApprovalResolveRequest resolucion) {
        if (resolucion == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
    }

    private ApprovalRequestResponse respuesta(ApprovalRequest solicitud) {
        return new ApprovalRequestResponse(solicitud.getId(), solicitud.getCourse().getId(), solicitud.getStatus(),
                solicitud.getRequestDate(), solicitud.getResolutionDate(), solicitud.getInstructorComment(),
                solicitud.getAdministratorComment(),
                solicitud.getAdministrator() == null ? null : solicitud.getAdministrator().getId());
    }
}
