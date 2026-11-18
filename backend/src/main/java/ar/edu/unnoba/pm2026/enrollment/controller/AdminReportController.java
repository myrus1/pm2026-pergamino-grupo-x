package ar.edu.unnoba.pm2026.enrollment.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.CourseRevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrollmentReportResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueReportResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentReportService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.web.DateRange;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#40): reportes del administrador. fromDate y toDate (YYYY-MM-DD, inclusive) son obligatorios;
// 400 si faltan, si no son fechas o si fromDate es posterior a toDate.
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final EnrollmentReportService enrollmentReportService;
    private final AuthorizationService authorizationService;

    public AdminReportController(EnrollmentReportService enrollmentReportService,
            AuthorizationService authorizationService) {
        this.enrollmentReportService = enrollmentReportService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/revenue")
    public RevenueReportResponse revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return enrollmentReportService.revenue(new DateRange(fromDate, toDate));
    }

    @GetMapping("/courses-revenue")
    public List<CourseRevenueResponse> coursesRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return enrollmentReportService.revenueByCourse(new DateRange(fromDate, toDate));
    }

    @GetMapping("/enrollments")
    public List<EnrollmentReportResponse> enrollments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return enrollmentReportService.enrollments(new DateRange(fromDate, toDate));
    }
}
