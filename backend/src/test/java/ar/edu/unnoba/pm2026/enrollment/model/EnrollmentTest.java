package ar.edu.unnoba.pm2026.enrollment.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

// Tests unitarios de la logica de negocio de Enrollment (sin contexto Spring ni BD)
class EnrollmentTest {

    private static final BigDecimal IMPORTE = new BigDecimal("100.00");

    private final Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
    private final Student otroEstudiante = new Student("Luis", "Gomez", "luis@example.com", "secret", "22222222");
    private final Course course = new Course("Curso", "Desc", IMPORTE, LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
            new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
    private final Course otherCourse = new Course("Otro curso", "Desc", IMPORTE, LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 20), 5, new Specialty("Bases de datos"),
            new Instructor("Sara", "Lopez", "sara@example.com", "secret"));

    @Test
    void constructorVinculaElEstudianteYElCursoEnAmbasDirecciones() {
        LocalDateTime date = LocalDateTime.of(2026, 5, 10, 10, 30);

        Enrollment enrollment = new Enrollment(estudiante, course, IMPORTE, date);

        assertThat(enrollment.getStudent()).isSameAs(estudiante);
        assertThat(enrollment.getCourse()).isSameAs(course);
        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo(IMPORTE);
        assertThat(enrollment.getEnrollmentDate()).isEqualTo(date);
        assertThat(estudiante.getEnrollments()).containsExactly(enrollment);
        assertThat(course.getEnrollments()).containsExactly(enrollment);
    }

    @Test
    void elInstructorRecibeEl65PorCientoDelPrecioBaseAunqueElAlumnoTengaDescuento() {
        Enrollment discounted = new Enrollment(estudiante, course, new BigDecimal("75.00"));

        assertThat(discounted.instructorRevenue()).isEqualByComparingTo("65.00");
    }

    @Test
    void laParteDelInstructorSeRedondeaACentavos() {
        Course oddCourse = new Course("Curso", "Desc", new BigDecimal("99.99"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));

        assertThat(new Enrollment(estudiante, oddCourse, new BigDecimal("99.99")).instructorRevenue())
                .isEqualByComparingTo("64.99");
    }

    @Test
    void setStudentMueveLaInscripcionAlNuevoEstudiante() {
        Enrollment enrollment = new Enrollment(estudiante, course, IMPORTE);

        enrollment.setStudent(otroEstudiante);

        assertThat(enrollment.getStudent()).isSameAs(otroEstudiante);
        assertThat(estudiante.getEnrollments()).isEmpty();
        assertThat(otroEstudiante.getEnrollments()).containsExactly(enrollment);
    }

    @Test
    void setCourseMueveLaInscripcionAlNuevoCurso() {
        Enrollment enrollment = new Enrollment(estudiante, course, IMPORTE);

        enrollment.setCourse(otherCourse);

        assertThat(enrollment.getCourse()).isSameAs(otherCourse);
        assertThat(course.getEnrollments()).isEmpty();
        assertThat(otherCourse.getEnrollments()).containsExactly(enrollment);
    }

    @Test
    void setStudentRechazaEstudianteNulo() {
        Enrollment enrollment = new Enrollment(estudiante, course, IMPORTE);

        assertThatThrownBy(() -> enrollment.setStudent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Estudiante no puede ser nulo");
    }

    @Test
    void setCourseRechazaCursoNulo() {
        Enrollment enrollment = new Enrollment(estudiante, course, IMPORTE);

        assertThatThrownBy(() -> enrollment.setCourse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Curso no puede ser nulo");
    }
}
