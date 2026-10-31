package com.example.demo.approval.repositories;

import com.example.demo.approval.model.ApprovalRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByCourseInstructorIdOrderByRequestDateDesc(Long instructorId);
}
