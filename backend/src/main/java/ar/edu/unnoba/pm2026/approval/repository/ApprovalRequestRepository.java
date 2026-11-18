package ar.edu.unnoba.pm2026.approval.repository;

import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByCourseInstructorIdOrderByRequestDateDesc(Long instructorId);

    // Las pendientes, de la mas vieja a la mas nueva: el admin las resuelve en orden de llegada
    List<ApprovalRequest> findByStatusOrderByRequestDateAsc(ApprovalStatus status);
}
