package com.example.demo.approval.services;

import com.example.demo.approval.model.ApprovalRequest;
import com.example.demo.approval.repositories.ApprovalRequestRepository;
import com.example.demo.course.services.CourseService;
import com.example.demo.user.services.AdministratorService;
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

    public ApprovalRequest submit(Long courseId, String instructorComment) {
        ApprovalRequest request = new ApprovalRequest(courseService.getById(courseId), instructorComment);
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
