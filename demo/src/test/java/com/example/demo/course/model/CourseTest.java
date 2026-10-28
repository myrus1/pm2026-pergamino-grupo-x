package com.example.demo.course.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Tests unitarios de la logica de negocio de Course (sin contexto Spring ni BD)
class CourseTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 20);
    private static final BigDecimal PRECIO = new BigDecimal("100.00");

    private final Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
    private final Specialty especialidad = new Specialty("Programacion");

    private Course curso(Specialty specialty, Instructor docente, Integer capacidad) {
        return new Course("Curso de Java", "Descripcion del curso", PRECIO, INICIO, FIN, capacidad, specialty, docente);
    }

    @Test
    void constructorGuardaLosDatosYVinculaInstructorYEspecialidad() {
        Course curso = curso(especialidad, instructor, 5);

        assertThat(curso.getTitle()).isEqualTo("Curso de Java");
        assertThat(curso.getDescription()).isEqualTo("Descripcion del curso");
        assertThat(curso.getBasePrice()).isEqualByComparingTo(PRECIO);
        assertThat(curso.getStartDate()).isEqualTo(INICIO);
        assertThat(curso.getEndDate()).isEqualTo(FIN);
        assertThat(curso.getMaxCapacity()).isEqualTo(5);
        assertThat(instructor.getCourses()).containsExactly(curso);
        assertThat(especialidad.getCourses()).containsExactly(curso);
    }

    @Test
    void cursoNuevoIniciaEnEstadoBorrador() {
        assertThat(curso(especialidad, instructor, 5).getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void constructorRechazaTituloNulo() {
        assertThatThrownBy(() -> new Course(null, "Desc", PRECIO, INICIO, FIN, 5, especialidad, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El titulo no puede ser nulo");
    }

    @Test
    void constructorRechazaPrecioNulo() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", null, INICIO, FIN, 5, especialidad, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El precio base no puede ser nulo");
    }

    @Test
    void constructorRechazaPrecioNegativo() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", new BigDecimal("-0.01"), INICIO, FIN, 5, especialidad, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El precio base no puede ser negativo");
    }

    @Test
    void constructorRechazaFechasNulas() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRECIO, null, FIN, 5, especialidad, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La fecha de inicio no puede ser nula");
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRECIO, INICIO, null, 5, especialidad, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La fecha de fin no puede ser nula");
    }

    @Test
    void constructorRechazaQueLaFechaFinNoSeaPosteriorALaDeInicio() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRECIO, INICIO, INICIO, 5, especialidad, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de fin debe ser posterior a la de inicio");
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRECIO, INICIO, INICIO.minusDays(1), 5, especialidad, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de fin debe ser posterior a la de inicio");
    }

    @Test
    void constructorRechazaCapacidadNula() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRECIO, INICIO, FIN, null, especialidad, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La capacidad no puede ser nula");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3})
    void constructorRechazaCapacidadNoPositiva(int capacidad) {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRECIO, INICIO, FIN, capacidad, especialidad, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La capacidad debe ser mayor a cero");
    }

    @Test
    void hasAvailableCapacityReflejaLosLugaresLibres() {
        Course curso = curso(especialidad, instructor, 2);

        assertThat(curso.hasAvailableCapacity()).isTrue();
        new Enrollment(new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"), curso, PRECIO);
        assertThat(curso.hasAvailableCapacity()).isTrue();
        new Enrollment(new Student("Luis", "Gomez", "luis@example.com", "secret", "22222222"), curso, PRECIO);
        assertThat(curso.hasAvailableCapacity()).isFalse();
    }

    @Test
    void hasAvailableCapacityEsFalseSinCapacidadDefinida() {
        Course curso = curso(especialidad, instructor, 5);
        curso.setMaxCapacity(null);

        assertThat(curso.hasAvailableCapacity()).isFalse();
    }

    @Test
    void cambiarDeEspecialidadActualizaAmbosLados() {
        Course curso = curso(especialidad, instructor, 5);
        Specialty nueva = new Specialty("Bases de datos");

        curso.setSpecialty(nueva);

        assertThat(curso.getSpecialty()).isSameAs(nueva);
        assertThat(especialidad.getCourses()).doesNotContain(curso);
        assertThat(nueva.getCourses()).containsExactly(curso);
    }

    @Test
    void setSpecialtyRechazaDejarElCursoSinEspecialidad() {
        Course curso = curso(especialidad, instructor, 5);

        assertThatThrownBy(() -> curso.setSpecialty(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La especialidad no puede ser nula");
    }

    @Test
    void cambiarDeInstructorActualizaAmbosLados() {
        Course curso = curso(especialidad, instructor, 5);
        Instructor nuevo = new Instructor("Sara", "Lopez", "sara@example.com", "secret");

        curso.setInstructor(nuevo);

        assertThat(curso.getInstructor()).isSameAs(nuevo);
        assertThat(instructor.getCourses()).doesNotContain(curso);
        assertThat(nuevo.getCourses()).containsExactly(curso);
    }

    @Test
    void setInstructorRechazaDejarElCursoSinInstructor() {
        Course curso = curso(especialidad, instructor, 5);

        assertThatThrownBy(() -> curso.setInstructor(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Instructor no puede ser nulo");
    }

    @Test
    void setStatusRechazaEstadoNulo() {
        Course curso = curso(especialidad, instructor, 5);

        assertThatThrownBy(() -> curso.setStatus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Estado no puede ser nulo");
    }

    @Test
    void removeEnrollmentQuitaLaInscripcionDelCurso() {
        Course curso = curso(especialidad, instructor, 5);
        Enrollment inscripcion = new Enrollment(
                new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"), curso, PRECIO);
        assertThat(curso.getEnrollments()).containsExactly(inscripcion);

        curso.removeEnrollment(inscripcion);

        assertThat(curso.getEnrollments()).isEmpty();
    }

    @Test
    void addEnrollmentIgnoraNulos() {
        Course curso = curso(especialidad, instructor, 5);

        curso.addEnrollment(null);

        assertThat(curso.getEnrollments()).isEmpty();
    }
}
