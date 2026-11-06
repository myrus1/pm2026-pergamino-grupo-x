package com.example.demo.approval.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.approval.model.ApprovalRequest;
import com.example.demo.approval.model.ApprovalRequestException;
import com.example.demo.approval.services.ApprovalRequestService;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApprovalRequestController.class)
class ApprovalRequestControllerTest {

    private static final String RUTA = "/api/approval-requests";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApprovalRequestService approvalRequestService;

    private ApprovalRequest solicitud(Long id) {
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        TestIds.fijarId(curso, 3L);
        ApprovalRequest solicitud = new ApprovalRequest(curso, "Listo");
        TestIds.fijarId(solicitud, id);
        return solicitud;
    }

    private Administrator admin() {
        Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "secret");
        TestIds.fijarId(admin, 9L);
        return admin;
    }

    @Test
    void getAllFiltraPorEstado() throws Exception {
        ApprovalRequest pendiente = solicitud(1L);
        ApprovalRequest aprobada = solicitud(2L);
        aprobada.approve(admin(), null);
        when(approvalRequestService.getAll()).thenReturn(List.of(pendiente, aprobada));

        mockMvc.perform(get(RUTA).param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].courseId").value(3));
    }

    @Test
    void getAllPorInstructorDevuelveSusSolicitudes() throws Exception {
        when(approvalRequestService.getByInstructor(4L)).thenReturn(List.of(solicitud(1L)));

        mockMvc.perform(get(RUTA).param("instructorId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].instructorComment").value("Listo"));
        verify(approvalRequestService, never()).getAll();
    }

    @Test
    void submitResponde201ConLaSolicitud() throws Exception {
        when(approvalRequestService.submit(3L, "Listo")).thenReturn(solicitud(1L));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"courseId":3,"instructorComment":"Listo"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.administratorId").doesNotExist());
    }

    @Test
    void submitSinCursoResponde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("courseId")));
    }

    @Test
    void submitDeCursoQueNoEstaEnBorradorResponde409() throws Exception {
        when(approvalRequestService.submit(3L, null)).thenThrow(
                new ApprovalRequestException("Solo se puede solicitar la aprobacion de un curso en borrador"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"courseId":3}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Solo se puede solicitar la aprobacion de un curso en borrador"));
    }

    @Test
    void approveResponde200ConLaResolucion() throws Exception {
        ApprovalRequest aprobada = solicitud(1L);
        aprobada.approve(admin(), "Ok");
        when(approvalRequestService.approve(1L, 9L, "Ok")).thenReturn(aprobada);

        mockMvc.perform(post(RUTA + "/1/approve").contentType(MediaType.APPLICATION_JSON).content("""
                {"administratorId":9,"administratorComment":"Ok"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.administratorId").value(9))
                .andExpect(jsonPath("$.resolutionDate").exists());
    }

    @Test
    void approveSinAdministradorResponde400() throws Exception {
        mockMvc.perform(post(RUTA + "/1/approve").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("administratorId")));
        verify(approvalRequestService, never()).approve(any(), any(), any());
    }

    @Test
    void rejectDeSolicitudYaResueltaResponde409() throws Exception {
        when(approvalRequestService.reject(1L, 9L, null))
                .thenThrow(new ApprovalRequestException("La solicitud ya fue resuelta"));

        mockMvc.perform(post(RUTA + "/1/reject").contentType(MediaType.APPLICATION_JSON).content("""
                {"administratorId":9}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La solicitud ya fue resuelta"));
    }

    @Test
    void rejectDeSolicitudInexistenteResponde404() throws Exception {
        when(approvalRequestService.reject(99L, 9L, null))
                .thenThrow(new EntityNotFoundException("Solicitud de aprobacion no encontrada con id 99"));

        mockMvc.perform(post(RUTA + "/99/reject").contentType(MediaType.APPLICATION_JSON).content("""
                {"administratorId":9}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Solicitud de aprobacion no encontrada con id 99"));
    }
}
