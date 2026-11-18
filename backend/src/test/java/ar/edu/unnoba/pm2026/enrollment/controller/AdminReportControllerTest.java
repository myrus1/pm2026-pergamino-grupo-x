package ar.edu.unnoba.pm2026.enrollment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.CourseRevenueResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.EnrollmentReportResponse;
import ar.edu.unnoba.pm2026.enrollment.dto.RevenueReportResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentReportService;
import ar.edu.unnoba.pm2026.shared.web.DateRange;
import ar.edu.unnoba.pm2026.testutil.AuthTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Contrato HTTP de /admin/reports (TP6, #40)
@WebMvcTest(AdminReportController.class)
class AdminReportControllerTest {

    private static final DateRange MAYO = new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentReportService enrollmentReportService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asDefaultAdministrator(authorizationService);
    }

    @Test
    void revenueDevuelveElTotalDelRango() throws Exception {
        when(enrollmentReportService.revenue(MAYO)).thenReturn(new RevenueReportResponse(MAYO.from(), MAYO.to(), 2,
                new BigDecimal("175.00")));

        mockMvc.perform(get("/admin/reports/revenue").param("fromDate", "2026-05-01").param("toDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromDate").value("2026-05-01"))
                .andExpect(jsonPath("$.enrollments").value(2))
                .andExpect(jsonPath("$.total").value(175.00));
    }

    @Test
    void coursesRevenueDevuelveInscriptosYTotalPorCurso() throws Exception {
        when(enrollmentReportService.revenueByCourse(MAYO)).thenReturn(List.of(
                new CourseRevenueResponse(7L, "Curso de Java", 2, new BigDecimal("175.00"))));

        mockMvc.perform(get("/admin/reports/courses-revenue").param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(7))
                .andExpect(jsonPath("$[0].enrollments").value(2))
                .andExpect(jsonPath("$[0].total").value(175.00));
    }

    @Test
    void enrollmentsDevuelvePrecioBaseYFinal() throws Exception {
        when(enrollmentReportService.enrollments(MAYO)).thenReturn(List.of(new EnrollmentReportResponse(2L,
                LocalDateTime.of(2026, 5, 10, 0, 0), 1L, "Ana", "Diaz", 7L, "Curso de Java", new BigDecimal("100.00"),
                new BigDecimal("75.00"), new BigDecimal("25.00"))));

        mockMvc.perform(get("/admin/reports/enrollments").param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].basePrice").value(100.00))
                .andExpect(jsonPath("$[0].finalAmount").value(75.00))
                .andExpect(jsonPath("$[0].discount").value(25.00));
    }

    @Test
    void sinFechaResponde400ConElParametroQueFalta() throws Exception {
        mockMvc.perform(get("/admin/reports/revenue").param("fromDate", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.missing-parameter"))
                .andExpect(jsonPath("$.detail").value("Falta el parametro 'toDate'"));
        verify(enrollmentReportService, never()).revenue(any());
    }

    @Test
    void conFechaInvalidaResponde400() throws Exception {
        mockMvc.perform(get("/admin/reports/revenue").param("fromDate", "01/05/2026").param("toDate", "2026-05-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid-parameter"))
                .andExpect(jsonPath("$.detail").value("El parametro 'fromDate' tiene un valor invalido"));
    }

    @Test
    void conFromDatePosteriorAToDateResponde400() throws Exception {
        mockMvc.perform(get("/admin/reports/enrollments").param("fromDate", "2026-06-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid-date-range"));
        verify(enrollmentReportService, never()).enrollments(any());
    }

    @Test
    void conTokenDeInstructorResponde403() throws Exception {
        AuthTestSupport.asInstructor(authorizationService, 2L);

        mockMvc.perform(get("/admin/reports/revenue").param("fromDate", "2026-05-01").param("toDate", "2026-05-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        AuthTestSupport.withoutToken(authorizationService);

        mockMvc.perform(get("/admin/reports/courses-revenue").param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isUnauthorized());
    }
}
