package ar.edu.unnoba.pm2026.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.enrollment.dto.CourseRevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrollmentReportResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueReportResponse;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.enrollment.repository.EnrollmentRepository;
import ar.edu.unnoba.pm2026.shared.web.DateRange;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentReportServiceTest {

    private static final DateRange MAYO = new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private EnrollmentReportService enrollmentReportService;

    private Course java;
    private Course figma;

    @BeforeEach
    void setUp() {
        enrollmentReportService = new EnrollmentReportService(enrollmentRepository);
        java = Fixtures.course(7L, CourseStatus.PUBLISHED);
        figma = Fixtures.course(8L, CourseStatus.PUBLISHED);
        figma.setTitle("Figma");
    }

    private Enrollment enrollment(Long id, Long studentId, Course course, String finalAmount) {
        Enrollment enrollment = new Enrollment(Fixtures.student(studentId), course, new BigDecimal(finalAmount),
                LocalDateTime.of(2026, 5, 10, 0, 0));
        TestIds.setId(enrollment, id);
        return enrollment;
    }

    private void enrollmentsInMay(Enrollment... enrollments) {
        when(enrollmentRepository.findByEnrollmentDateRange(LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0))).thenReturn(List.of(enrollments));
    }

    @Test
    void revenueSumaElPrecioFinalDeLasInscripcionesDelRango() {
        enrollmentsInMay(enrollment(1L, 1L, java, "100.00"), enrollment(2L, 2L, java, "75.00"));

        RevenueReportResponse report = enrollmentReportService.revenue(MAYO);

        assertThat(report.enrollments()).isEqualTo(2);
        assertThat(report.total()).isEqualByComparingTo("175.00");
        assertThat(report.fromDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void revenueSinInscripcionesDaCero() {
        enrollmentsInMay();

        assertThat(enrollmentReportService.revenue(MAYO).total()).isEqualByComparingTo("0");
    }

    @Test
    void revenueByCourseAgrupaPorCursoYOrdenaDelQueMasFacturo() {
        enrollmentsInMay(enrollment(1L, 1L, figma, "90.00"), enrollment(2L, 1L, java, "100.00"),
                enrollment(3L, 2L, java, "75.00"));

        List<CourseRevenueResponse> report = enrollmentReportService.revenueByCourse(MAYO);

        assertThat(report).containsExactly(new CourseRevenueResponse(7L, "Curso de Java", 2, new BigDecimal("175.00")),
                new CourseRevenueResponse(8L, "Figma", 1, new BigDecimal("90.00")));
    }

    @Test
    void enrollmentsMuestraPrecioBasePrecioFinalYDescuento() {
        enrollmentsInMay(enrollment(2L, 1L, java, "75.00"));

        List<EnrollmentReportResponse> report = enrollmentReportService.enrollments(MAYO);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).basePrice()).isEqualByComparingTo("100.00");
        assertThat(report.get(0).finalAmount()).isEqualByComparingTo("75.00");
        assertThat(report.get(0).discount()).isEqualByComparingTo("25.00");
        assertThat(report.get(0).courseTitle()).isEqualTo("Curso de Java");
        assertThat(report.get(0).studentName()).isEqualTo("Ana");
    }

    @Test
    void instructorRevenueFiltraPorElRangoYCalculaEl65PorCientoDelPrecioBase() {
        Enrollment adentro = enrollment(1L, 1L, java, "75.00");
        Enrollment afuera = new Enrollment(Fixtures.student(2L), java, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 6, 2, 0, 0));
        TestIds.setId(afuera, 2L);
        when(enrollmentRepository.findByCourseInstructorId(2L)).thenReturn(List.of(afuera, adentro));

        var report = enrollmentReportService.instructorRevenue(2L, MAYO);

        assertThat(report.enrollments()).hasSize(1);
        assertThat(report.enrollments().get(0).enrollmentId()).isEqualTo(1L);
        assertThat(report.enrollments().get(0).amount()).isEqualByComparingTo("65.00");
        assertThat(report.total()).isEqualByComparingTo("65.00");
    }

    @Test
    void instructorRevenueSinFechasIncluyeTodasYLasOrdenaPorFecha() {
        Enrollment junio = new Enrollment(Fixtures.student(2L), java, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 6, 2, 0, 0));
        TestIds.setId(junio, 2L);
        when(enrollmentRepository.findByCourseInstructorId(2L)).thenReturn(List.of(junio,
                enrollment(1L, 1L, java, "75.00")));

        var report = enrollmentReportService.instructorRevenue(2L, new DateRange(null, null));

        assertThat(report.enrollments()).extracting("enrollmentId").containsExactly(1L, 2L);
        assertThat(report.total()).isEqualByComparingTo("130.00");
    }
}
