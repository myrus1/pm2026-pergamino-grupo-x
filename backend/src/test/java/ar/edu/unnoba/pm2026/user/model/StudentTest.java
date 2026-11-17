package ar.edu.unnoba.pm2026.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

// Tests unitarios de la logica de negocio de Student (sin contexto Spring ni BD)
class StudentTest {

    private static final LocalDate HOY = LocalDate.of(2026, 5, 10);
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private final Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
    private final Specialty specialty = new Specialty("Programacion");

    // Curso publicado que comienza despues de la fecha de inscripcion pasada por defecto
    private Course publishedCourse(Specialty specialty, BigDecimal price, LocalDate inicio, Integer capacity) {
        Course course = new Course("Curso", "Desc", price, inicio, inicio.plusWeeks(4), capacity, specialty,
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        course.setStatus(CourseStatus.PUBLISHED);
        return course;
    }

    private Course defaultPublishedCourse() {
        return publishedCourse(specialty, PRICE, HOY.plusWeeks(2), 5);
    }

    @Test
    void applyCourseCreaLaInscripcionConElPrecioBase() {
        Course course = defaultPublishedCourse();

        Enrollment enrollment = estudiante.applyCourse(course, HOY);

        assertThat(enrollment.getStudent()).isSameAs(estudiante);
        assertThat(enrollment.getCourse()).isSameAs(course);
        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo("100.00");
        assertThat(enrollment.getEnrollmentDate()).isEqualTo(HOY.atStartOfDay());
        assertThat(estudiante.getEnrollments()).containsExactly(enrollment);
        assertThat(course.getEnrollments()).containsExactly(enrollment);
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
    void applyCourseRechazaCursosNoPublicados(CourseStatus sqlState) {
        Course course = defaultPublishedCourse();
        course.setStatus(sqlState);

        assertThatThrownBy(() -> estudiante.applyCourse(course, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("El curso no esta publicado");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5})
    void applyCourseRechazaInscripcionesDesdeElDiaDeInicio(int daysAfterStart) {
        LocalDate inicio = HOY.plusWeeks(2);
        Course course = publishedCourse(specialty, PRICE, inicio, 5);
        LocalDate date = inicio.plusDays(daysAfterStart);

        assertThatThrownBy(() -> estudiante.applyCourse(course, date))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("La inscripcion debe ser antes de que comience el curso");
    }

    @Test
    void applyCourseRechazaCuandoElCursoNoTieneCupo() {
        Course course = publishedCourse(specialty, PRICE, HOY.plusWeeks(2), 1);
        Student otro = new Student("Luis", "Gomez", "luis@example.com", "secret", "22222222");
        otro.applyCourse(course, HOY);

        assertThatThrownBy(() -> estudiante.applyCourse(course, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("El curso esta lleno");
    }

    @Test
    void applyCourseRechazaInscripcionDuplicadaAlMismoCurso() {
        Course course = defaultPublishedCourse();
        estudiante.applyCourse(course, HOY);

        assertThatThrownBy(() -> estudiante.applyCourse(course, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("Estudiante inscripto en el curso");
    }

    @Test
    void applyCourseRechazaInscripcionEnOtraInstanciaDelMismoCurso() {
        Course savedCourse = defaultPublishedCourse();
        Course reloadedCourse = defaultPublishedCourse();
        TestIds.setId(savedCourse, 42L);
        TestIds.setId(reloadedCourse, 42L);
        estudiante.applyCourse(savedCourse, HOY);

        assertThatThrownBy(() -> estudiante.applyCourse(reloadedCourse, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("Estudiante inscripto en el curso");
    }

    @Test
    void applyCourseAplicaDescuentoPorMismaEspecialidadEnElMismoMes() {
        Course firstCourse = publishedCourse(specialty, PRICE, LocalDate.of(2026, 5, 15), 5);
        Course secondCourse = publishedCourse(specialty, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(firstCourse, LocalDate.of(2026, 5, 3));

        Enrollment enrollment = estudiante.applyCourse(secondCourse, LocalDate.of(2026, 5, 20));

        // 80.00 * 0.75 = 60.00 (25% de descuento por fidelidad)
        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo("60.00");
    }

    static Stream<LocalDate> fechasPreviasSinDescuento() {
        // Un mes antes del segundo curso (mismo anio, mes distinto)
        // y el mismo mes pero un anio antes: en ninguno de los dos casos hay descuento
        return Stream.of(LocalDate.of(2026, 4, 20), LocalDate.of(2025, 5, 3));
    }

    @ParameterizedTest
    @MethodSource("fechasPreviasSinDescuento")
    void applyCourseCobraElPrecioCompletoSiLaInscripcionPreviaNoFueEnElMismoMesYAnio(LocalDate previousDate) {
        Course firstCourse = publishedCourse(specialty, PRICE, previousDate.plusDays(7), 5);
        Course secondCourse = publishedCourse(specialty, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(firstCourse, previousDate);

        Enrollment enrollment = estudiante.applyCourse(secondCourse, LocalDate.of(2026, 5, 20));

        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void applyCourseCobraElPrecioCompletoParaOtraEspecialidad() {
        Specialty otherSpecialty = new Specialty("Bases de datos");
        Course firstCourse = publishedCourse(otherSpecialty, PRICE, LocalDate.of(2026, 5, 15), 5);
        Course secondCourse = publishedCourse(specialty, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(firstCourse, LocalDate.of(2026, 5, 3));

        Enrollment enrollment = estudiante.applyCourse(secondCourse, LocalDate.of(2026, 5, 20));

        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void applyCourseReconoceLaMismaEspecialidadPorId() {
        // Dos instancias de la especialidad como las que devuelve JPA tras recargar la BD
        Specialty reloadedSpecialty = new Specialty("Programacion");
        TestIds.setId(specialty, 7L);
        TestIds.setId(reloadedSpecialty, 7L);
        Course firstCourse = publishedCourse(specialty, PRICE, LocalDate.of(2026, 5, 15), 5);
        Course secondCourse = publishedCourse(reloadedSpecialty, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(firstCourse, LocalDate.of(2026, 5, 3));

        Enrollment enrollment = estudiante.applyCourse(secondCourse, LocalDate.of(2026, 5, 20));

        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void applyCourseRechazaArgumentosNulos() {
        assertThatThrownBy(() -> estudiante.applyCourse(null, HOY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("curso no puede ser nulo");
        assertThatThrownBy(() -> estudiante.applyCourse(defaultPublishedCourse(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La fecha de inscripcion no puede ser nula");
    }

    @Test
    void applyCourseRechazaCursoSinEspecialidad() {
        // El constructor de Course acepta especialidad nula (por eso este escenario es alcanzable)
        Course course = new Course("Curso", "Desc", PRICE, HOY.plusWeeks(2), HOY.plusWeeks(6), 5, null,
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        course.setStatus(CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> estudiante.applyCourse(course, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("Especialidad del curso no definida");
    }

    @Test
    void applyCourseRechazaCursoSinPrecioBase() {
        Course course = defaultPublishedCourse();
        course.setBasePrice(null);

        assertThatThrownBy(() -> estudiante.applyCourse(course, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("No se establecio el precio base del curso");
    }
}
