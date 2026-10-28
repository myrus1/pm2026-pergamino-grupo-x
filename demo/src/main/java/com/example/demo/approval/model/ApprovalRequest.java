package com.example.demo.approval.model;

import com.example.demo.course.model.Course;
import com.example.demo.user.model.Administrator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

// Entidad para la solicitud de revision de un curso

@Entity
@Table(name = "approval_requests")
@Getter
@Setter

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
    public ApprovalRequest(Course course) {
        this.course = course;
    }

}
