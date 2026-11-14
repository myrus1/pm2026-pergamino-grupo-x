package ar.edu.unnoba.pm2026.course.model;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.unnoba.pm2026.user.model.Instructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Tests unitarios de la logica de negocio de Specialty (sin contexto Spring ni BD)
class SpecialtyTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 20);

    private final Specialty specialty = new Specialty("Programacion");
    private final Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");

    @Test
    void addInstructorMantieneLaRelacionEnAmbasDirecciones() {
        specialty.addInstructor(instructor);

        assertThat(specialty.getInstructors()).containsExactly(instructor);
        assertThat(instructor.getSpecialties()).containsExactly(specialty);
    }

    @Test
    void removeInstructorQuitaLaRelacionEnAmbasDirecciones() {
        specialty.addInstructor(instructor);

        specialty.removeInstructor(instructor);

        assertThat(specialty.getInstructors()).isEmpty();
        assertThat(instructor.getSpecialties()).isEmpty();
    }

    @Test
    void setInstructorsReemplazaLosAnteriores() {
        specialty.addInstructor(instructor);
        Instructor updated = new Instructor("Sara", "Lopez", "sara@example.com", "secret");

        specialty.setInstructors(Set.of(updated));

        assertThat(specialty.getInstructors()).containsExactly(updated);
        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(updated.getSpecialties()).containsExactly(specialty);
    }

    @Test
    void addCourseAsignaLaEspecialidadAlCurso() {
        Specialty anterior = new Specialty("Bases de datos");
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5, anterior, instructor);

        specialty.addCourse(course);

        assertThat(course.getSpecialty()).isSameAs(specialty);
        assertThat(specialty.getCourses()).containsExactly(course);
        assertThat(anterior.getCourses()).isEmpty();
    }

    @Test
    void removeCourseQuitaElCursoDeLaColeccion() {
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5, specialty, instructor);

        specialty.removeCourse(course);

        assertThat(specialty.getCourses()).isEmpty();
    }
}
