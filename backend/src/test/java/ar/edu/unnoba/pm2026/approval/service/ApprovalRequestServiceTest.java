package ar.edu.unnoba.pm2026.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.approval.model.ApprovalRequest;
import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import ar.edu.unnoba.pm2026.approval.model.ApprovalStatus;
import ar.edu.unnoba.pm2026.approval.repository.ApprovalRequestRepository;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
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

    private Course course() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.setId(instructor, 2L);
        return new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"), instructor);
    }

    private AuthenticatedUser administrator() {
        return new AuthenticatedUser(3L, "ada@example.com", Role.ADMINISTRATOR);
    }

    private AuthenticatedUser instructor(Long id) {
        return new AuthenticatedUser(id, "iris@example.com", Role.INSTRUCTOR);
    }

    private Course courseOfInstructor(Long instructorId) {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.setId(instructor, instructorId);
        return new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"), instructor);
    }

    @Test
    void submitCreaLaSolicitudYPasaElCursoAPendiente() {
        Course course = course();
        when(courseService.getById(1L)).thenReturn(course);
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequest request = approvalRequestService.submit(1L, "Listo", instructor(2L));

        assertThat(request.getCourse()).isSameAs(course);
        assertThat(request.getInstructorComment()).isEqualTo("Listo");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void submitDeUnCursoPublicadoNoGuardaNada() {
        Course course = course();
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseService.getById(1L)).thenReturn(course);

        assertThatThrownBy(() -> approvalRequestService.submit(1L, null, instructor(2L)))
                .isInstanceOf(ApprovalRequestException.class);
        verify(approvalRequestRepository, never()).save(any());
    }

    @Test
    void submitComoInstructorDeCursoAjenoNoGuardaNada() {
        when(courseService.getById(1L)).thenReturn(courseOfInstructor(5L));

        assertThatThrownBy(() -> approvalRequestService.submit(1L, "Listo", instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El curso pertenece a otro instructor");
        verify(approvalRequestRepository, never()).save(any());
    }

    @Test
    void getByIdDeInstructorConSolicitudAjenaLanzaExcepcion() {
        ApprovalRequest request = new ApprovalRequest(courseOfInstructor(5L), null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> approvalRequestService.getById(5L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("La solicitud de aprobacion no le pertenece");
    }

    @Test
    void getByIdDeAdministradorConSolicitudAjenaLaDevuelve() {
        ApprovalRequest request = new ApprovalRequest(courseOfInstructor(5L), null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThat(approvalRequestService.getById(5L, administrator())).isSameAs(request);
    }

    @Test
    void approvePublicaElCursoConElAdministrador() {
        Course course = course();
        ApprovalRequest request = new ApprovalRequest(course, null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(administratorService.getById(9L)).thenReturn(admin);
        when(approvalRequestRepository.save(request)).thenReturn(request);

        approvalRequestService.approve(5L, 9L, "Ok");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(request.getAdministrator()).isSameAs(admin);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void rejectDevuelveElCursoABorrador() {
        Course course = course();
        ApprovalRequest request = new ApprovalRequest(course, null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(administratorService.getById(9L)).thenReturn(admin);
        when(approvalRequestRepository.save(request)).thenReturn(request);

        approvalRequestService.reject(5L, 9L, "Corregir");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void approveConAdministradorInexistenteNoCambiaNada() {
        Course course = course();
        ApprovalRequest request = new ApprovalRequest(course, null);
        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(administratorService.getById(99L))
                .thenThrow(new EntityNotFoundException("Administrador no encontrado con id 99"));

        assertThatThrownBy(() -> approvalRequestService.approve(5L, 99L, null))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
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
