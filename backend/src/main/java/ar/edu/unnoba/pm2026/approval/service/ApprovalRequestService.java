package ar.edu.unnoba.pm2026.approval.service;

import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.repository.ApprovalRequestRepository;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final CourseService courseService;
    private final AdministratorService administratorService;

    public ApprovalRequestService(ApprovalRequestRepository approvalRequestRepository, CourseService courseService,
            AdministratorService administratorService) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.courseService = courseService;
        this.administratorService = administratorService;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getAll() {
        return approvalRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getByInstructor(Long instructorId) {
        return approvalRequestRepository.findByCourseInstructorIdOrderByRequestDateDesc(instructorId);
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getById(Long id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud de aprobacion no encontrada con id " + id));
    }

    // El instructor solo consulta sus solicitudes; el administrador, cualquiera.
    @Transactional(readOnly = true)
    public ApprovalRequest getById(Long id, AuthenticatedUser actor) {
        ApprovalRequest request = getById(id);
        if (actor.role() != Role.ADMINISTRATOR) {
            actor.requireOwnership(request.getCourse().getInstructor().getId(),
                    "La solicitud de aprobacion no le pertenece");
        }
        return request;
    }

    // Solo el dueño del curso puede enviarlo a aprobacion.
    public ApprovalRequest submit(Long courseId, String instructorComment, AuthenticatedUser actor) {
        Course course = courseService.getById(courseId);
        actor.requireOwnership(course.getInstructor().getId(), "El curso pertenece a otro instructor");
        ApprovalRequest request = new ApprovalRequest(course, instructorComment);
        return approvalRequestRepository.save(request);
    }

    public ApprovalRequest approve(Long id, Long administratorId, String administratorComment) {
        ApprovalRequest request = getById(id);
        request.approve(administratorService.getById(administratorId), administratorComment);
        return approvalRequestRepository.save(request);
    }

    public ApprovalRequest reject(Long id, Long administratorId, String administratorComment) {
        ApprovalRequest request = getById(id);
        request.reject(administratorService.getById(administratorId), administratorComment);
        return approvalRequestRepository.save(request);
    }
}
