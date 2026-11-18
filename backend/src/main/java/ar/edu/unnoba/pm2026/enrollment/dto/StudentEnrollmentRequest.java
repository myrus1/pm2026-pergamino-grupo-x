package ar.edu.unnoba.pm2026.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// TP6: POST /student/enrollments (CreateEnrollmentRequestDTO: courseId). El alumno sale del token.
@Getter
@Setter
public class StudentEnrollmentRequest {

    @NotNull
    private Long courseId;
}
