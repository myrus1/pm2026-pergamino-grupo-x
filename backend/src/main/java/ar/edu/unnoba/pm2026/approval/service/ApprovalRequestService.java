package ar.edu.unnoba.pm2026.approval.service;

import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import ar.edu.unnoba.pm2026.approval.repository.ApprovalRequestRepository;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.observability.BusinessMetrics;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final CourseService courseService;
    private final AdministratorService administratorService;
    private final BusinessMetrics businessMetrics;

    public ApprovalRequestService(ApprovalRequestRepository approvalRequestRepository, CourseService courseService,
            AdministratorService administratorService, BusinessMetrics businessMetrics) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.courseService = courseService;
        this.administratorService = administratorService;
        this.businessMetrics = businessMetrics;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getAll() {
        return approvalRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getByInstructor(Long instructorId) {
        return approvalRequestRepository.findByCourseInstructorIdOrderByRequestDateDesc(instructorId);
    }

    // TP6: GET /admin/approval-requests muestra solo las que esperan respuesta
    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPending() {
        return approvalRequestRepository.findByStatusOrderByRequestDateAsc(ApprovalStatus.SUBMITTED);
    }

    // TP6: PUT /admin/approval-requests/{id} decide con el status del body (APPROVED o REJECTED)
    public ApprovalRequest resolve(Long id, ApprovalStatus decision, Long administratorId,
            String administratorComment) {
        if (decision == ApprovalStatus.APPROVED) {
            return approve(id, administratorId, administratorComment);
        }
        if (decision == ApprovalStatus.REJECTED) {
            return reject(id, administratorId, administratorComment);
        }
        throw new ValidationException(ErrorCode.APPROVAL_REQUEST_INVALID_DECISION);
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getById(Long id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPROVAL_REQUEST_NOT_FOUND, id));
    }

    // El instructor solo consulta sus solicitudes; el administrador, cualquiera.
    @Transactional(readOnly = true)
    public ApprovalRequest getById(Long id, AuthenticatedUser actor) {
        ApprovalRequest request = getById(id);
        if (actor.role() != Role.ADMINISTRATOR) {
            actor.requireOwnership(request.getCourse().getInstructor().getId(),
                    ErrorCode.AUTHORIZATION_APPROVAL_REQUEST_NOT_OWNER);
        }
        return request;
    }

    // Solo el instructor dueño del curso puede enviarlo a aprobacion; el administrador solo resuelve.
    public ApprovalRequest submit(Long courseId, String instructorComment, AuthenticatedUser actor) {
        if (actor.role() != Role.INSTRUCTOR) {
            throw new AuthorizationException(ErrorCode.AUTHORIZATION_APPROVAL_INSTRUCTOR_ONLY);
        }
        Course course = courseService.getById(courseId);
        actor.requireOwnership(course.getInstructor().getId(), ErrorCode.AUTHORIZATION_COURSE_NOT_OWNER);
        ApprovalRequest request = new ApprovalRequest(course, instructorComment);
        return approvalRequestRepository.save(request);
    }

    public ApprovalRequest approve(Long id, Long administratorId, String administratorComment) {
        ApprovalRequest request = getById(id);
        request.approve(administratorService.getById(administratorId), administratorComment);
        ApprovalRequest saved = approvalRequestRepository.save(request);
        businessMetrics.courseApproved();
        return saved;
    }

    public ApprovalRequest reject(Long id, Long administratorId, String administratorComment) {
        ApprovalRequest request = getById(id);
        request.reject(administratorService.getById(administratorId), administratorComment);
        ApprovalRequest saved = approvalRequestRepository.save(request);
        businessMetrics.courseRejected();
        return saved;
    }
}
