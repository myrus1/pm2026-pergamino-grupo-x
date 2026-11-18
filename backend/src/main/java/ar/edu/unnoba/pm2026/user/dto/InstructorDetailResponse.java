package ar.edu.unnoba.pm2026.user.dto;

import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import java.util.List;

// TP6: GET /admin/instructors/{id}: datos del instructor y resumen de sus cursos (cantidad por estado
// y la lista). Se arma dentro de una transaccion (InstructorService) porque recorre relaciones LAZY.
public record InstructorDetailResponse(Long id, String name, String lastName, String email,
        List<SpecialtyResponse> specialties, CourseSummary courseSummary, List<CourseResponse> courses) {

    public record CourseSummary(int total, int draft, int pendingApproval, int published) {
    }
}
