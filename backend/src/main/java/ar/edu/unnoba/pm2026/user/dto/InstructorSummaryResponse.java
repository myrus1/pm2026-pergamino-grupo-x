package ar.edu.unnoba.pm2026.user.dto;

import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import java.util.List;

// TP6: un instructor en GET /admin/instructors, con sus especialidades.
// Las especialidades son LAZY: se arma dentro de una transaccion (InstructorService).
public record InstructorSummaryResponse(Long id, String name, String lastName, String email,
        List<SpecialtyResponse> specialties) {
}
