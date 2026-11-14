package ar.edu.unnoba.pm2026.approval.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.approval.dto.ApprovalRequestResponse;
import ar.edu.unnoba.pm2026.approval.dto.ApprovalResolveRequest;
import ar.edu.unnoba.pm2026.approval.dto.ApprovalSubmitRequest;
import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import ar.edu.unnoba.pm2026.approval.service.ApprovalRequestService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
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
        AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR, Role.INSTRUCTOR);
        List<ApprovalRequest> requests;
        if (actor.role() == Role.INSTRUCTOR) {
            requests = approvalRequestService.getByInstructor(actor.id());
        } else {
            requests = instructorId == null
                    ? approvalRequestService.getAll()
                    : approvalRequestService.getByInstructor(instructorId);
        }
        return requests.stream()
                .filter(request -> status == null || request.getStatus() == status)
                .map(this::response)
                .toList();
    }

    @GetMapping("/{id}")
    public ApprovalRequestResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR, Role.INSTRUCTOR);
        return response(approvalRequestService.getById(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalRequestResponse submit(@RequestBody ApprovalSubmitRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.getCourseId(), "courseId");
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR, Role.ADMINISTRATOR);
        return response(approvalRequestService.submit(request.getCourseId(), request.getInstructorComment(),
                actor));
    }

    @PostMapping("/{id}/approve")
    public ApprovalRequestResponse approve(@PathVariable Long id, @RequestBody ApprovalResolveRequest resolution,
            @RequestHeader(name = "Authorization", required = false) String token) {
        validate(resolution);
        AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR);
        return response(approvalRequestService.approve(id, actor.id(), resolution.getAdministratorComment()));
    }

    @PostMapping("/{id}/reject")
    public ApprovalRequestResponse reject(@PathVariable Long id, @RequestBody ApprovalResolveRequest resolution,
            @RequestHeader(name = "Authorization", required = false) String token) {
        validate(resolution);
        AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR);
        return response(approvalRequestService.reject(id, actor.id(), resolution.getAdministratorComment()));
    }

    private void validate(ApprovalResolveRequest resolution) {
        if (resolution == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
    }

    private ApprovalRequestResponse response(ApprovalRequest request) {
        return new ApprovalRequestResponse(request.getId(), request.getCourse().getId(), request.getStatus(),
                request.getRequestDate(), request.getResolutionDate(), request.getInstructorComment(),
                request.getAdministratorComment(),
                request.getAdministrator() == null ? null : request.getAdministrator().getId());
    }
}
