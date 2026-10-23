package com.example.demo.course.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.user.model.Instructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

// Tests unitarios de la logica de negocio de Specialty (sin contexto Spring ni BD)
class SpecialtyTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 20);

    private final Specialty especialidad = new Specialty("Programacion");
    private final Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");

    @Test
    void addInstructorMantieneLaRelacionEnAmbasDirecciones() {
        especialidad.addInstructor(instructor);

        assertThat(especialidad.getInstructors()).containsExactly(instructor);
        assertThat(instructor.getSpecialties()).containsExactly(especialidad);
    }

    @Test
    void removeInstructorQuitaLaRelacionEnAmbasDirecciones() {
        especialidad.addInstructor(instructor);

        especialidad.removeInstructor(instructor);

        assertThat(especialidad.getInstructors()).isEmpty();
        assertThat(instructor.getSpecialties()).isEmpty();
    }

    @Test
    void setInstructorsReemplazaLosAnteriores() {
        especialidad.addInstructor(instructor);
        Instructor nuevo = new Instructor("Sara", "Lopez", "sara@example.com", "secret");

        especialidad.setInstructors(Set.of(nuevo));

        assertThat(especialidad.getInstructors()).containsExactly(nuevo);
        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(nuevo.getSpecialties()).containsExactly(especialidad);
    }

    @Test
    void addCourseAsignaLaEspecialidadAlCurso() {
        Specialty anterior = new Specialty("Bases de datos");
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5, anterior, instructor);

        especialidad.addCourse(curso);

        assertThat(curso.getSpecialty()).isSameAs(especialidad);
        assertThat(especialidad.getCourses()).containsExactly(curso);
        assertThat(anterior.getCourses()).isEmpty();
    }

    @Test
    void removeCourseQuitaElCursoDeLaColeccion() {
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), INICIO, FIN, 5, especialidad, instructor);

        especialidad.removeCourse(curso);

        assertThat(especialidad.getCourses()).isEmpty();
    }
}
