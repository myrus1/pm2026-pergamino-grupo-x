package com.example.demo.approval.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.approval.model.ApprovalRequest;
import com.example.demo.approval.model.ApprovalRequestException;
import com.example.demo.approval.model.ApprovalStatus;
import com.example.demo.approval.repositories.ApprovalRequestRepository;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.CourseService;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.AdministratorService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private AdministratorService administratorService;

    private ApprovalRequestService approvalRequestService;

    private final Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "secret");

    @BeforeEach
    void setUp() {
        approvalRequestService = new ApprovalRequestService(approvalRequestRepository, courseService,
                administratorService);
    }

    private Course curso() {
        return new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
    }

    @Test
    void submitCreaLaSolicitudYPasaElCursoAPendiente() {
        Course curso = curso();
        when(courseService.getById(1L)).thenReturn(curso);
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequest solicitud = approvalRequestService.submit(1L, "Listo");

        assertThat(solicitud.getCourse()).isSameAs(curso);
        assertThat(solicitud.getInstructorComment()).isEqualTo("Listo");
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void submitDeUnCursoPublicadoNoGuardaNada() {
        Course curso = curso();
        curso.setStatus(CourseStatus.PUBLISHED);
        when(courseService.getById(1L)).thenReturn(curso);

        assertThatThrownBy(() -> approvalRequestService.submit(1L, null))
                .isInstanceOf(ApprovalRequestException.class);
        verify(approvalRequestRepository, never()).save(any());
    }

    @Test
    void approvePublicaElCursoConElAdministrador() {
        Course curso = curso();
        ApprovalRequest solicitud = new ApprovalRequest(curso, null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(solicitud));
        when(administratorService.getById(9L)).thenReturn(admin);
        when(approvalRequestRepository.save(solicitud)).thenReturn(solicitud);

        approvalRequestService.approve(5L, 9L, "Ok");

        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(solicitud.getAdministrator()).isSameAs(admin);
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void rejectDevuelveElCursoABorrador() {
        Course curso = curso();
        ApprovalRequest solicitud = new ApprovalRequest(curso, null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(solicitud));
        when(administratorService.getById(9L)).thenReturn(admin);
        when(approvalRequestRepository.save(solicitud)).thenReturn(solicitud);

        approvalRequestService.reject(5L, 9L, "Corregir");

        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void approveConAdministradorInexistenteNoCambiaNada() {
        Course curso = curso();
        ApprovalRequest solicitud = new ApprovalRequest(curso, null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(solicitud));
        when(administratorService.getById(99L))
                .thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        assertThatThrownBy(() -> approvalRequestService.approve(5L, 99L, null))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
        verify(approvalRequestRepository, never()).save(any());
    }

    @Test
    void getByIdLanzaExceptionCuandoLaSolicitudNoExiste() {
        when(approvalRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvalRequestService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Solicitud de aprobacion no encontrada con id 99");
    }
}
