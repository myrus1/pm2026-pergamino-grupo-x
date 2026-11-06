package com.example.demo.approval.model;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.user.model.Administrator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

// Entidad para la solicitud de revision de un curso

@Entity
@Table(name = "approval_requests")
@Getter

public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime requestDate = LocalDateTime.now();

    private LocalDateTime resolutionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.SUBMITTED;

    private String instructorComment;

    private String administratorComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Administrator administrator;

    public ApprovalRequest() {
    }

    // Constructor para iniciar una solicitud sobre un curso
    public ApprovalRequest(Course course, String instructorComment) {
        Objects.requireNonNull(course, "El curso no puede ser nulo");
        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new ApprovalRequestException("Solo se puede solicitar la aprobacion de un curso en borrador");
        }
        this.course = course;
        this.instructorComment = instructorComment;
        course.setStatus(CourseStatus.PENDING_APPROVAL);
    }

    public void approve(Administrator administrator, String administratorComment) {
        resolve(administrator, administratorComment, ApprovalStatus.APPROVED);
        course.setStatus(CourseStatus.PUBLISHED);
    }

    public void reject(Administrator administrator, String administratorComment) {
        resolve(administrator, administratorComment, ApprovalStatus.REJECTED);
        course.setStatus(CourseStatus.DRAFT);
    }

    private void resolve(Administrator administrator, String administratorComment, ApprovalStatus newStatus) {
        Objects.requireNonNull(administrator, "El administrador no puede ser nulo");
        if (status != ApprovalStatus.SUBMITTED) {
            throw new ApprovalRequestException("La solicitud ya fue resuelta");
        }
        this.status = newStatus;
        this.administrator = administrator;
        this.administratorComment = administratorComment;
        this.resolutionDate = LocalDateTime.now();
    }
}
