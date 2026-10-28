package com.example.demo.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.enrollment.model.CourseEnrollmentException;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.testutil.TestIds;
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
    private static final BigDecimal PRECIO = new BigDecimal("100.00");

    private final Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
    private final Specialty especialidad = new Specialty("Programacion");

    // Curso publicado que comienza despues de la fecha de inscripcion pasada por defecto
    private Course cursoPublicado(Specialty specialty, BigDecimal precio, LocalDate inicio, Integer capacidad) {
        Course curso = new Course("Curso", "Desc", precio, inicio, inicio.plusWeeks(4), capacidad, specialty,
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        curso.setStatus(CourseStatus.PUBLISHED);
        return curso;
    }

    private Course cursoPublicadoPorDefecto() {
        return cursoPublicado(especialidad, PRECIO, HOY.plusWeeks(2), 5);
    }

    @Test
    void applyCourseCreaLaInscripcionConElPrecioBase() {
        Course curso = cursoPublicadoPorDefecto();

        Enrollment inscripcion = estudiante.applyCourse(curso, HOY);

        assertThat(inscripcion.getStudent()).isSameAs(estudiante);
        assertThat(inscripcion.getCourse()).isSameAs(curso);
        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo("100.00");
        assertThat(inscripcion.getEnrollmentDate()).isEqualTo(HOY.atStartOfDay());
        assertThat(estudiante.getEnrollments()).containsExactly(inscripcion);
        assertThat(curso.getEnrollments()).containsExactly(inscripcion);
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
    void applyCourseRechazaCursosNoPublicados(CourseStatus estado) {
        Course curso = cursoPublicadoPorDefecto();
        curso.setStatus(estado);

        assertThatThrownBy(() -> estudiante.applyCourse(curso, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("El curso no esta publicado");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5})
    void applyCourseRechazaInscripcionesDesdeElDiaDeInicio(int diasDespuesDelInicio) {
        LocalDate inicio = HOY.plusWeeks(2);
        Course curso = cursoPublicado(especialidad, PRECIO, inicio, 5);
        LocalDate fecha = inicio.plusDays(diasDespuesDelInicio);

        assertThatThrownBy(() -> estudiante.applyCourse(curso, fecha))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("La inscripcion debe ser antes de que comience el curso");
    }

    @Test
    void applyCourseRechazaCuandoElCursoNoTieneCupo() {
        Course curso = cursoPublicado(especialidad, PRECIO, HOY.plusWeeks(2), 1);
        Student otro = new Student("Luis", "Gomez", "luis@example.com", "secret", "22222222");
        otro.applyCourse(curso, HOY);

        assertThatThrownBy(() -> estudiante.applyCourse(curso, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("El curso esta lleno");
    }

    @Test
    void applyCourseRechazaInscripcionDuplicadaAlMismoCurso() {
        Course curso = cursoPublicadoPorDefecto();
        estudiante.applyCourse(curso, HOY);

        assertThatThrownBy(() -> estudiante.applyCourse(curso, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("Estudiante inscripto en el curso");
    }

    @Test
    void applyCourseRechazaInscripcionEnOtraInstanciaDelMismoCurso() {
        Course cursoGuardado = cursoPublicadoPorDefecto();
        Course cursoVueltoACargar = cursoPublicadoPorDefecto();
        TestIds.fijarId(cursoGuardado, 42L);
        TestIds.fijarId(cursoVueltoACargar, 42L);
        estudiante.applyCourse(cursoGuardado, HOY);

        assertThatThrownBy(() -> estudiante.applyCourse(cursoVueltoACargar, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("Estudiante inscripto en el curso");
    }

    @Test
    void applyCourseAplicaDescuentoPorMismaEspecialidadEnElMismoMes() {
        Course primerCurso = cursoPublicado(especialidad, PRECIO, LocalDate.of(2026, 5, 15), 5);
        Course segundoCurso = cursoPublicado(especialidad, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(primerCurso, LocalDate.of(2026, 5, 3));

        Enrollment inscripcion = estudiante.applyCourse(segundoCurso, LocalDate.of(2026, 5, 20));

        // 80.00 * 0.75 = 60.00 (25% de descuento por fidelidad)
        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo("60.00");
    }

    static Stream<LocalDate> fechasPreviasSinDescuento() {
        // Un mes antes del segundo curso (mismo anio, mes distinto)
        // y el mismo mes pero un anio antes: en ninguno de los dos casos hay descuento
        return Stream.of(LocalDate.of(2026, 4, 20), LocalDate.of(2025, 5, 3));
    }

    @ParameterizedTest
    @MethodSource("fechasPreviasSinDescuento")
    void applyCourseCobraElPrecioCompletoSiLaInscripcionPreviaNoFueEnElMismoMesYAnio(LocalDate fechaPrevia) {
        Course primerCurso = cursoPublicado(especialidad, PRECIO, fechaPrevia.plusDays(7), 5);
        Course segundoCurso = cursoPublicado(especialidad, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(primerCurso, fechaPrevia);

        Enrollment inscripcion = estudiante.applyCourse(segundoCurso, LocalDate.of(2026, 5, 20));

        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void applyCourseCobraElPrecioCompletoParaOtraEspecialidad() {
        Specialty otraEspecialidad = new Specialty("Bases de datos");
        Course primerCurso = cursoPublicado(otraEspecialidad, PRECIO, LocalDate.of(2026, 5, 15), 5);
        Course segundoCurso = cursoPublicado(especialidad, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(primerCurso, LocalDate.of(2026, 5, 3));

        Enrollment inscripcion = estudiante.applyCourse(segundoCurso, LocalDate.of(2026, 5, 20));

        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void applyCourseReconoceLaMismaEspecialidadPorId() {
        // Dos instancias de la especialidad como las que devuelve JPA tras recargar la BD
        Specialty especialidadRecargada = new Specialty("Programacion");
        TestIds.fijarId(especialidad, 7L);
        TestIds.fijarId(especialidadRecargada, 7L);
        Course primerCurso = cursoPublicado(especialidad, PRECIO, LocalDate.of(2026, 5, 15), 5);
        Course segundoCurso = cursoPublicado(especialidadRecargada, new BigDecimal("80.00"), LocalDate.of(2026, 5, 25), 5);
        estudiante.applyCourse(primerCurso, LocalDate.of(2026, 5, 3));

        Enrollment inscripcion = estudiante.applyCourse(segundoCurso, LocalDate.of(2026, 5, 20));

        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void applyCourseRechazaArgumentosNulos() {
        assertThatThrownBy(() -> estudiante.applyCourse(null, HOY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("curso no puede ser nulo");
        assertThatThrownBy(() -> estudiante.applyCourse(cursoPublicadoPorDefecto(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("La fecha de inscripcion no puede ser nula");
    }

    @Test
    void applyCourseRechazaCursoSinEspecialidad() {
        // El constructor de Course acepta especialidad nula (por eso este escenario es alcanzable)
        Course curso = new Course("Curso", "Desc", PRECIO, HOY.plusWeeks(2), HOY.plusWeeks(6), 5, null,
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        curso.setStatus(CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> estudiante.applyCourse(curso, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("Especialidad del curso no definida");
    }

    @Test
    void applyCourseRechazaCursoSinPrecioBase() {
        Course curso = cursoPublicadoPorDefecto();
        curso.setBasePrice(null);

        assertThatThrownBy(() -> estudiante.applyCourse(curso, HOY))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("No se establecio el precio base del curso");
    }
}
