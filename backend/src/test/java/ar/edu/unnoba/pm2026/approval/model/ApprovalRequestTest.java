package ar.edu.unnoba.pm2026.approval.model;

import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApprovalRequestTest {

    private final Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "secret");

    private Course course() {
        return new Course("Curso de Java", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
    }

    @Test
    void solicitarAprobacionPasaElCursoAPendiente() {
        Course course = course();

        ApprovalRequest request = new ApprovalRequest(course, "Listo para revisar");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
        assertThat(request.getInstructorComment()).isEqualTo("Listo para revisar");
        assertThat(request.getRequestDate()).isNotNull();
        assertThat(request.getResolutionDate()).isNull();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"PENDING_APPROVAL", "PUBLISHED"})
    void soloSePuedeSolicitarConElCursoEnBorrador(CourseStatus sqlState) {
        Course course = course();
        course.setStatus(sqlState);

        assertThatThrownBy(() -> new ApprovalRequest(course, null))
                .isInstanceOf(ApprovalRequestException.class)
                .hasMessage("Solo se puede solicitar la aprobacion de un curso en borrador");
        assertThat(course.getStatus()).isEqualTo(sqlState);
    }

    @Test
    void solicitarAprobacionRechazaCursoNulo() {
        assertThatThrownBy(() -> new ApprovalRequest(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El curso no puede ser nulo");
    }

    @Test
    void aprobarPublicaElCursoYRegistraLaResolucion() {
        Course course = course();
        ApprovalRequest request = new ApprovalRequest(course, null);

        request.approve(admin, "Aprobado");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(request.getAdministrator()).isSameAs(admin);
        assertThat(request.getAdministratorComment()).isEqualTo("Aprobado");
        assertThat(request.getResolutionDate()).isNotNull();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void rechazarDevuelveElCursoABorrador() {
        Course course = course();
        ApprovalRequest request = new ApprovalRequest(course, null);

        request.reject(admin, "Falta la descripcion");

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(request.getAdministratorComment()).isEqualTo("Falta la descripcion");
        assertThat(request.getResolutionDate()).isNotNull();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void trasUnRechazoSePuedeVolverASolicitar() {
        Course course = course();
        new ApprovalRequest(course, null).reject(admin, "Corregir");

        ApprovalRequest nueva = new ApprovalRequest(course, "Corregido");

        assertThat(nueva.getStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void unaSolicitudResueltaNoSePuedeVolverAResolver() {
        Course course = course();
        ApprovalRequest request = new ApprovalRequest(course, null);
        request.approve(admin, null);

        assertThatThrownBy(() -> request.reject(admin, null))
                .isInstanceOf(ApprovalRequestException.class)
                .hasMessage("La solicitud ya fue resuelta");
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void resolverRechazaAdministradorNulo() {
        ApprovalRequest request = new ApprovalRequest(course(), null);

        assertThatThrownBy(() -> request.approve(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El administrador no puede ser nulo");
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
    }
}
