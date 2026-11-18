package ar.edu.unnoba.pm2026.enrollment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.enrollment.dto.InstructorRevenueReportResponse;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentReportService;
import ar.edu.unnoba.pm2026.enrollment.service.EnrollmentService;
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

// Contrato HTTP de las ganancias del instructor (TP6, #41)
@WebMvcTest(InstructorRevenueController.class)
class InstructorRevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private EnrollmentReportService enrollmentReportService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @BeforeEach
    void authorizeByDefault() {
        AuthTestSupport.asInstructor(authorizationService, 2L);
    }

    @Test
    void meRevenueDevuelveElAcumuladoDelInstructorDelToken() throws Exception {
        when(enrollmentService.instructorRevenue(2L)).thenReturn(new BigDecimal("130.00"));

        mockMvc.perform(get("/instructors/me/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(130.00));
    }

    @Test
    void revenueEnrollmentsConRangoDevuelveTotalYDetalle() throws Exception {
        DateRange mayo = new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        when(enrollmentReportService.instructorRevenue(2L, mayo)).thenReturn(new InstructorRevenueReportResponse(
                mayo.from(), mayo.to(), new BigDecimal("65.00"), List.of(new InstructorRevenueReportResponse
                        .EnrollmentRevenue(11L, LocalDateTime.of(2026, 5, 10, 0, 0), 7L, "Curso de Java",
                        new BigDecimal("65.00")))));

        mockMvc.perform(get("/instructor/revenue/enrollments").param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(65.00))
                .andExpect(jsonPath("$.enrollments[0].courseTitle").value("Curso de Java"))
                .andExpect(jsonPath("$.enrollments[0].amount").value(65.00));
    }

    @Test
    void revenueEnrollmentsSinFechasTomaTodo() throws Exception {
        when(enrollmentReportService.instructorRevenue(2L, new DateRange(null, null)))
                .thenReturn(new InstructorRevenueReportResponse(null, null, BigDecimal.ZERO, List.of()));

        mockMvc.perform(get("/instructor/revenue/enrollments")).andExpect(status().isOk());
        verify(enrollmentReportService).instructorRevenue(2L, new DateRange(null, null));
    }

    @Test
    void revenueEnrollmentsConFromDatePosteriorResponde400() throws Exception {
        mockMvc.perform(get("/instructor/revenue/enrollments").param("fromDate", "2026-06-01")
                        .param("toDate", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid-date-range"));
        verify(enrollmentReportService, never()).instructorRevenue(any(), any());
    }

    @Test
    void conTokenDeAlumnoResponde403() throws Exception {
        AuthTestSupport.asStudent(authorizationService, 1L);

        mockMvc.perform(get("/instructors/me/revenue")).andExpect(status().isForbidden());
    }
}
