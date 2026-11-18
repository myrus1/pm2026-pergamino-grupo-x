package ar.edu.unnoba.pm2026.enrollment.service;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.enrollment.dto.CourseRevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrollmentReportResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueReportResponse;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.enrollment.repository.EnrollmentRepository;
import ar.edu.unnoba.pm2026.shared.web.DateRange;
import ar.edu.unnoba.pm2026.user.model.Student;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reportes de recaudacion del TP6 (#40). Recorren relaciones LAZY (curso y alumno de cada inscripcion),
// por eso las respuestas se arman aca, dentro de la transaccion.
@Service
@Transactional(readOnly = true)
public class EnrollmentReportService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentReportService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public RevenueReportResponse revenue(DateRange range) {
        List<Enrollment> enrollments = enrollmentsIn(range);
        return new RevenueReportResponse(range.from(), range.to(), enrollments.size(), totalPaid(enrollments));
    }

    // Solo los cursos que tuvieron inscripciones en el rango, del que mas facturo al que menos
    public List<CourseRevenueResponse> revenueByCourse(DateRange range) {
        Map<Course, List<Enrollment>> byCourse = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollmentsIn(range)) {
            byCourse.computeIfAbsent(enrollment.getCourse(), course -> new ArrayList<>()).add(enrollment);
        }
        return byCourse.entrySet().stream()
                .map(entry -> new CourseRevenueResponse(entry.getKey().getId(), entry.getKey().getTitle(),
                        entry.getValue().size(), totalPaid(entry.getValue())))
                .sorted(Comparator.comparing(CourseRevenueResponse::total).reversed()
                        .thenComparing(CourseRevenueResponse::title))
                .toList();
    }

    public List<EnrollmentReportResponse> enrollments(DateRange range) {
        return enrollmentsIn(range).stream().map(EnrollmentReportService::reportOf).toList();
    }

    private List<Enrollment> enrollmentsIn(DateRange range) {
        return enrollmentRepository.findByEnrollmentDateRange(range.startInclusive(), range.endExclusive());
    }

    private static BigDecimal totalPaid(List<Enrollment> enrollments) {
        return enrollments.stream().map(Enrollment::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static EnrollmentReportResponse reportOf(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        Student student = enrollment.getStudent();
        return new EnrollmentReportResponse(enrollment.getId(), enrollment.getEnrollmentDate(), student.getId(),
                student.getFirstName(), student.getLastName(), course.getId(), course.getTitle(),
                course.getBasePrice(), enrollment.getFinalAmount(),
                course.getBasePrice().subtract(enrollment.getFinalAmount()));
    }
}
