package ar.edu.unnoba.pm2026.enrollment.controller;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.InstructorRevenueReportResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentReportService;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.web.DateRange;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#41): lo que gana el instructor del token: el 65% del precio base de cada inscripcion a sus cursos.
@SecurityRequirement(name = "token")
@RestController
public class InstructorRevenueController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentReportService enrollmentReportService;
    private final AuthorizationService authorizationService;

    public InstructorRevenueController(EnrollmentService enrollmentService,
            EnrollmentReportService enrollmentReportService, AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.enrollmentReportService = enrollmentReportService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/instructors/me/revenue")
    public RevenueResponse revenue(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return new RevenueResponse(enrollmentService.instructorRevenue(actor.id()));
    }

    // fromDate y toDate son opcionales (YYYY-MM-DD, inclusive); 400 si fromDate es posterior a toDate
    @GetMapping("/instructor/revenue/enrollments")
    public InstructorRevenueReportResponse revenueByEnrollment(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.INSTRUCTOR);
        return enrollmentReportService.instructorRevenue(actor.id(), new DateRange(fromDate, toDate));
    }
}
