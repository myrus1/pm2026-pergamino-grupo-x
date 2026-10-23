package com.example.demo.enrollment.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

// Tests unitarios de la logica de negocio de Enrollment (sin contexto Spring ni BD)
class EnrollmentTest {

    private static final BigDecimal IMPORTE = new BigDecimal("100.00");

    private final Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
    private final Student otroEstudiante = new Student("Luis", "Gomez", "luis@example.com", "secret", "22222222");
    private final Course curso = new Course("Curso", "Desc", IMPORTE, LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
            new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
    private final Course otroCurso = new Course("Otro curso", "Desc", IMPORTE, LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 20), 5, new Specialty("Bases de datos"),
            new Instructor("Sara", "Lopez", "sara@example.com", "secret"));

    @Test
    void constructorVinculaElEstudianteYElCursoEnAmbasDirecciones() {
        LocalDateTime fecha = LocalDateTime.of(2026, 5, 10, 10, 30);

        Enrollment inscripcion = new Enrollment(estudiante, curso, IMPORTE, fecha);

        assertThat(inscripcion.getStudent()).isSameAs(estudiante);
        assertThat(inscripcion.getCourse()).isSameAs(curso);
        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo(IMPORTE);
        assertThat(inscripcion.getEnrollmentDate()).isEqualTo(fecha);
        assertThat(estudiante.getEnrollments()).containsExactly(inscripcion);
        assertThat(curso.getEnrollments()).containsExactly(inscripcion);
    }

    @Test
    void setStudentMueveLaInscripcionAlNuevoEstudiante() {
        Enrollment inscripcion = new Enrollment(estudiante, curso, IMPORTE);

        inscripcion.setStudent(otroEstudiante);

        assertThat(inscripcion.getStudent()).isSameAs(otroEstudiante);
        assertThat(estudiante.getEnrollments()).isEmpty();
        assertThat(otroEstudiante.getEnrollments()).containsExactly(inscripcion);
    }

    @Test
    void setCourseMueveLaInscripcionAlNuevoCurso() {
        Enrollment inscripcion = new Enrollment(estudiante, curso, IMPORTE);

        inscripcion.setCourse(otroCurso);

        assertThat(inscripcion.getCourse()).isSameAs(otroCurso);
        assertThat(curso.getEnrollments()).isEmpty();
        assertThat(otroCurso.getEnrollments()).containsExactly(inscripcion);
    }

    @Test
    void setStudentRechazaEstudianteNulo() {
        Enrollment inscripcion = new Enrollment(estudiante, curso, IMPORTE);

        assertThatThrownBy(() -> inscripcion.setStudent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Estudiante no puede ser nulo");
    }

    @Test
    void setCourseRechazaCursoNulo() {
        Enrollment inscripcion = new Enrollment(estudiante, curso, IMPORTE);

        assertThatThrownBy(() -> inscripcion.setCourse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Curso no puede ser nulo");
    }
}
