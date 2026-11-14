package ar.edu.unnoba.pm2026.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Tests unitarios de la logica de negocio de Instructor (sin contexto Spring ni BD)
class InstructorTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 20);

    private final Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
    private final Specialty specialty = new Specialty("Programacion");

    @Test
    void createCourseCreaElCursoCuandoElInstructorTieneLaEspecialidad() {
        instructor.addSpecialty(specialty);

        Course course = instructor.createCourse("Curso de Java", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5,
                specialty);

        assertThat(course.getInstructor()).isSameAs(instructor);
        assertThat(course.getSpecialty()).isSameAs(specialty);
        assertThat(instructor.getCourses()).containsExactly(course);
        assertThat(specialty.getCourses()).containsExactly(course);
    }

    @Test
    void createCourseRechazaEspecialidadNula() {
        assertThatThrownBy(() -> instructor.createCourse("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5,
                null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("especialidad no puede ser nula");
    }

    @Test
    void createCourseRechazaEspecialidadQueElInstructorNoPosee() {
        Specialty otherSpecialty = new Specialty("Bases de datos");

        assertThatThrownBy(() -> instructor.createCourse("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5,
                otherSpecialty))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("instructor debe tener la especialidad del curso");
        assertThat(instructor.getCourses()).isEmpty();
    }

    @Test
    void validateSpecialtyReconoceLaMismaEspecialidadPorId() {
        Specialty own = new Specialty("Programacion");
        TestIds.setId(own, 1L);
        instructor.addSpecialty(own);
        Specialty sameById = new Specialty("Programacion");
        TestIds.setId(sameById, 1L);
        Specialty otra = new Specialty("Diseno");
        TestIds.setId(otra, 2L);

        instructor.validateSpecialty(sameById);
        assertThatThrownBy(() -> instructor.validateSpecialty(otra))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addSpecialtyMantieneLaRelacionEnAmbasDirecciones() {
        instructor.addSpecialty(specialty);

        assertThat(instructor.getSpecialties()).containsExactly(specialty);
        assertThat(specialty.getInstructors()).containsExactly(instructor);
    }

    @Test
    void removeSpecialtyQuitaLaRelacionEnAmbasDirecciones() {
        instructor.addSpecialty(specialty);

        instructor.removeSpecialty(specialty);

        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(specialty.getInstructors()).isEmpty();
    }

    @Test
    void setSpecialtiesReemplazaLasAnteriores() {
        instructor.addSpecialty(specialty);
        Specialty nueva = new Specialty("Bases de datos");

        instructor.setSpecialties(Set.of(nueva));

        assertThat(instructor.getSpecialties()).containsExactly(nueva);
        assertThat(specialty.getInstructors()).isEmpty();
        assertThat(nueva.getInstructors()).containsExactly(instructor);
    }

    @Test
    void setSpecialtiesNuloDejaLasEspecialidadesVacias() {
        instructor.addSpecialty(specialty);

        instructor.setSpecialties(null);

        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(specialty.getInstructors()).isEmpty();
    }
}
