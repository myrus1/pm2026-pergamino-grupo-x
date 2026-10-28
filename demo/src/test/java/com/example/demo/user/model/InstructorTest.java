package com.example.demo.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Tests unitarios de la logica de negocio de Instructor (sin contexto Spring ni BD)
class InstructorTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 20);

    private final Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
    private final Specialty especialidad = new Specialty("Programacion");

    @Test
    void createCourseCreaElCursoCuandoElInstructorTieneLaEspecialidad() {
        instructor.addSpecialty(especialidad);

        Course curso = instructor.createCourse("Curso de Java", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5,
                especialidad);

        assertThat(curso.getInstructor()).isSameAs(instructor);
        assertThat(curso.getSpecialty()).isSameAs(especialidad);
        assertThat(instructor.getCourses()).containsExactly(curso);
        assertThat(especialidad.getCourses()).containsExactly(curso);
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
        Specialty otraEspecialidad = new Specialty("Bases de datos");

        assertThatThrownBy(() -> instructor.createCourse("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5,
                otraEspecialidad))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("instructor debe tener la especialidad del curso");
        assertThat(instructor.getCourses()).isEmpty();
    }

    @Test
    void addSpecialtyMantieneLaRelacionEnAmbasDirecciones() {
        instructor.addSpecialty(especialidad);

        assertThat(instructor.getSpecialties()).containsExactly(especialidad);
        assertThat(especialidad.getInstructors()).containsExactly(instructor);
    }

    @Test
    void removeSpecialtyQuitaLaRelacionEnAmbasDirecciones() {
        instructor.addSpecialty(especialidad);

        instructor.removeSpecialty(especialidad);

        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(especialidad.getInstructors()).isEmpty();
    }

    @Test
    void setSpecialtiesReemplazaLasAnteriores() {
        instructor.addSpecialty(especialidad);
        Specialty nueva = new Specialty("Bases de datos");

        instructor.setSpecialties(Set.of(nueva));

        assertThat(instructor.getSpecialties()).containsExactly(nueva);
        assertThat(especialidad.getInstructors()).isEmpty();
        assertThat(nueva.getInstructors()).containsExactly(instructor);
    }

    @Test
    void setSpecialtiesNuloDejaLasEspecialidadesVacias() {
        instructor.addSpecialty(especialidad);

        instructor.setSpecialties(null);

        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(especialidad.getInstructors()).isEmpty();
    }
}
