package com.example.demo.approval.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApprovalRequestTest {

    private final Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "secret");

    private Course curso() {
        return new Course("Curso de Java", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
    }

    @Test
    void solicitarAprobacionPasaElCursoAPendiente() {
        Course curso = curso();

        ApprovalRequest solicitud = new ApprovalRequest(curso, "Listo para revisar");

        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
        assertThat(solicitud.getInstructorComment()).isEqualTo("Listo para revisar");
        assertThat(solicitud.getRequestDate()).isNotNull();
        assertThat(solicitud.getResolutionDate()).isNull();
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"PENDING_APPROVAL", "PUBLISHED"})
    void soloSePuedeSolicitarConElCursoEnBorrador(CourseStatus estado) {
        Course curso = curso();
        curso.setStatus(estado);

        assertThatThrownBy(() -> new ApprovalRequest(curso, null))
                .isInstanceOf(ApprovalRequestException.class)
                .hasMessage("Solo se puede solicitar la aprobacion de un curso en borrador");
        assertThat(curso.getStatus()).isEqualTo(estado);
    }

    @Test
    void solicitarAprobacionRechazaCursoNulo() {
        assertThatThrownBy(() -> new ApprovalRequest(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El curso no puede ser nulo");
    }

    @Test
    void aprobarPublicaElCursoYRegistraLaResolucion() {
        Course curso = curso();
        ApprovalRequest solicitud = new ApprovalRequest(curso, null);

        solicitud.approve(admin, "Aprobado");

        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(solicitud.getAdministrator()).isSameAs(admin);
        assertThat(solicitud.getAdministratorComment()).isEqualTo("Aprobado");
        assertThat(solicitud.getResolutionDate()).isNotNull();
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void rechazarDevuelveElCursoABorrador() {
        Course curso = curso();
        ApprovalRequest solicitud = new ApprovalRequest(curso, null);

        solicitud.reject(admin, "Falta la descripcion");

        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(solicitud.getAdministratorComment()).isEqualTo("Falta la descripcion");
        assertThat(solicitud.getResolutionDate()).isNotNull();
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void trasUnRechazoSePuedeVolverASolicitar() {
        Course curso = curso();
        new ApprovalRequest(curso, null).reject(admin, "Corregir");

        ApprovalRequest nueva = new ApprovalRequest(curso, "Corregido");

        assertThat(nueva.getStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void unaSolicitudResueltaNoSePuedeVolverAResolver() {
        Course curso = curso();
        ApprovalRequest solicitud = new ApprovalRequest(curso, null);
        solicitud.approve(admin, null);

        assertThatThrownBy(() -> solicitud.reject(admin, null))
                .isInstanceOf(ApprovalRequestException.class)
                .hasMessage("La solicitud ya fue resuelta");
        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void resolverRechazaAdministradorNulo() {
        ApprovalRequest solicitud = new ApprovalRequest(curso(), null);

        assertThatThrownBy(() -> solicitud.approve(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El administrador no puede ser nulo");
        assertThat(solicitud.getStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
    }
}
