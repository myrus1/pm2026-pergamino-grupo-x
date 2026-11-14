package ar.edu.unnoba.pm2026.course.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Tests unitarios de la logica de negocio de Course (sin contexto Spring ni BD)
class CourseTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 6, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 20);
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private final Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
    private final Specialty specialty = new Specialty("Programacion");

    private Course course(Specialty specialty, Instructor docente, Integer capacity) {
        return new Course("Curso de Java", "Descripcion del curso", PRICE, INICIO, FIN, capacity, specialty, docente);
    }

    @Test
    void constructorGuardaLosDatosYVinculaInstructorYEspecialidad() {
        Course course = course(specialty, instructor, 5);

        assertThat(course.getTitle()).isEqualTo("Curso de Java");
        assertThat(course.getDescription()).isEqualTo("Descripcion del curso");
        assertThat(course.getBasePrice()).isEqualByComparingTo(PRICE);
        assertThat(course.getStartDate()).isEqualTo(INICIO);
        assertThat(course.getEndDate()).isEqualTo(FIN);
        assertThat(course.getMaxCapacity()).isEqualTo(5);
        assertThat(instructor.getCourses()).containsExactly(course);
        assertThat(specialty.getCourses()).containsExactly(course);
    }

    @Test
    void cursoNuevoIniciaEnEstadoBorrador() {
        assertThat(course(specialty, instructor, 5).getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void constructorRechazaTituloNulo() {
        assertThatThrownBy(() -> new Course(null, "Desc", PRICE, INICIO, FIN, 5, specialty, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El titulo no puede ser nulo");
    }

    @Test
    void constructorRechazaPrecioNulo() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", null, INICIO, FIN, 5, specialty, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El precio base no puede ser nulo");
    }

    @Test
    void constructorRechazaPrecioNegativo() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", new BigDecimal("-0.01"), INICIO, FIN, 5, specialty, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El precio base no puede ser negativo");
    }

    @Test
    void constructorRechazaFechasNulas() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRICE, null, FIN, 5, specialty, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La fecha de inicio no puede ser nula");
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRICE, INICIO, null, 5, specialty, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La fecha de fin no puede ser nula");
    }

    @Test
    void constructorRechazaQueLaFechaFinNoSeaPosteriorALaDeInicio() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRICE, INICIO, INICIO, 5, specialty, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de fin debe ser posterior a la de inicio");
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRICE, INICIO, INICIO.minusDays(1), 5, specialty, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de fin debe ser posterior a la de inicio");
    }

    @Test
    void constructorRechazaCapacidadNula() {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRICE, INICIO, FIN, null, specialty, instructor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La capacidad no puede ser nula");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3})
    void constructorRechazaCapacidadNoPositiva(int capacity) {
        assertThatThrownBy(() -> new Course("Curso", "Desc", PRICE, INICIO, FIN, capacity, specialty, instructor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La capacidad debe ser mayor a cero");
    }

    @Test
    void hasAvailableCapacityReflejaLosLugaresLibres() {
        Course course = course(specialty, instructor, 2);

        assertThat(course.hasAvailableCapacity()).isTrue();
        new Enrollment(new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"), course, PRICE);
        assertThat(course.hasAvailableCapacity()).isTrue();
        new Enrollment(new Student("Luis", "Gomez", "luis@example.com", "secret", "22222222"), course, PRICE);
        assertThat(course.hasAvailableCapacity()).isFalse();
    }

    @Test
    void hasAvailableCapacityEsFalseSinCapacidadDefinida() {
        Course course = course(specialty, instructor, 5);
        course.setMaxCapacity(null);

        assertThat(course.hasAvailableCapacity()).isFalse();
    }

    @Test
    void cambiarDeEspecialidadActualizaAmbosLados() {
        Course course = course(specialty, instructor, 5);
        Specialty nueva = new Specialty("Bases de datos");

        course.setSpecialty(nueva);

        assertThat(course.getSpecialty()).isSameAs(nueva);
        assertThat(specialty.getCourses()).doesNotContain(course);
        assertThat(nueva.getCourses()).containsExactly(course);
    }

    @Test
    void setSpecialtyRechazaDejarElCursoSinEspecialidad() {
        Course course = course(specialty, instructor, 5);

        assertThatThrownBy(() -> course.setSpecialty(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La especialidad no puede ser nula");
    }

    @Test
    void cambiarDeInstructorActualizaAmbosLados() {
        Course course = course(specialty, instructor, 5);
        Instructor updated = new Instructor("Sara", "Lopez", "sara@example.com", "secret");

        course.setInstructor(updated);

        assertThat(course.getInstructor()).isSameAs(updated);
        assertThat(instructor.getCourses()).doesNotContain(course);
        assertThat(updated.getCourses()).containsExactly(course);
    }

    @Test
    void setInstructorRechazaDejarElCursoSinInstructor() {
        Course course = course(specialty, instructor, 5);

        assertThatThrownBy(() -> course.setInstructor(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Instructor no puede ser nulo");
    }

    @Test
    void setStatusRechazaEstadoNulo() {
        Course course = course(specialty, instructor, 5);

        assertThatThrownBy(() -> course.setStatus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Estado no puede ser nulo");
    }

    @Test
    void removeEnrollmentQuitaLaInscripcionDelCurso() {
        Course course = course(specialty, instructor, 5);
        Enrollment enrollment = new Enrollment(
                new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"), course, PRICE);
        assertThat(course.getEnrollments()).containsExactly(enrollment);

        course.removeEnrollment(enrollment);

        assertThat(course.getEnrollments()).isEmpty();
    }

    @Test
    void addEnrollmentIgnoraNulos() {
        Course course = course(specialty, instructor, 5);

        course.addEnrollment(null);

        assertThat(course.getEnrollments()).isEmpty();
    }
}
