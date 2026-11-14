package ar.edu.unnoba.pm2026.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.enrollment.repository.EnrollmentRepository;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    @Mock
    private InstructorService instructorService;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, studentService, courseService,
                instructorService);
    }

    private Enrollment enrollment(String basePrice, String paid) {
        Course course = new Course("Curso", "Desc", new BigDecimal(basePrice), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        return new Enrollment(new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"), course,
                new BigDecimal(paid));
    }

    @Test
    void totalRevenueSumaLoQuePagaronLosAlumnos() {
        when(enrollmentRepository.findAll()).thenReturn(List.of(enrollment("100.00", "100.00"),
                enrollment("80.00", "60.00")));

        assertThat(enrollmentService.totalRevenue()).isEqualByComparingTo("160.00");
    }

    @Test
    void totalRevenueSinInscripcionesEsCero() {
        when(enrollmentRepository.findAll()).thenReturn(List.of());

        assertThat(enrollmentService.totalRevenue()).isEqualByComparingTo("0");
    }

    @Test
    void instructorRevenueSumaEl65PorCientoDelPrecioBaseDeCadaInscripcion() {
        when(enrollmentRepository.findByCourseInstructorId(4L)).thenReturn(List.of(enrollment("100.00", "100.00"),
                enrollment("80.00", "60.00")));

        assertThat(enrollmentService.instructorRevenue(4L)).isEqualByComparingTo("117.00");
    }

    @Test
    void getStudentsOfCourseDevuelveLosInscriptosDelCurso() {
        Student student = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        when(enrollmentRepository.findStudentsByCourseId(3L)).thenReturn(List.of(student));

        assertThat(enrollmentService.getStudentsOfCourse(3L)).containsExactly(student);
        verify(courseService).getById(3L);
    }

    @Test
    void getStudentsOfCourseConCursoInexistenteLanzaExcepcion() {
        when(courseService.getById(99L)).thenThrow(new EntityNotFoundException("Curso no encontrado con id 99"));

        assertThatThrownBy(() -> enrollmentService.getStudentsOfCourse(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCoursesOfStudentDevuelveLosCursosDelAlumno() {
        Course course = futureCourse(CourseStatus.PUBLISHED);
        when(enrollmentRepository.findCoursesByStudentId(8L)).thenReturn(List.of(course));

        assertThat(enrollmentService.getCoursesOfStudent(8L)).containsExactly(course);
        verify(studentService).getById(8L);
    }

    @Test
    void getCoursesOfStudentConAlumnoInexistenteLanzaExcepcion() {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        assertThatThrownBy(() -> enrollmentService.getCoursesOfStudent(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void instructorRevenueDeInstructorInexistenteLanzaExcepcion() {
        when(instructorService.getById(99L)).thenThrow(new EntityNotFoundException("Instructor no encontrado con id 99"));

        assertThatThrownBy(() -> enrollmentService.instructorRevenue(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Instructor no encontrado con id 99");
    }

    private Course futureCourse(CourseStatus sqlState) {
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.now().plusWeeks(2),
                LocalDate.now().plusWeeks(6), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        course.setStatus(sqlState);
        return course;
    }

    @Test
    void enrollAplicaLasReglasDelAlumnoYGuardaLaInscripcion() {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        Course course = futureCourse(CourseStatus.PUBLISHED);
        when(studentService.getById(1L)).thenReturn(estudiante);
        when(courseService.getById(2L)).thenReturn(course);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment enrollment = enrollmentService.enroll(1L, 2L);

        assertThat(enrollment.getStudent()).isSameAs(estudiante);
        assertThat(enrollment.getCourse()).isSameAs(course);
        assertThat(enrollment.getFinalAmount()).isEqualByComparingTo("100.00");
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void enrollNoGuardaNadaSiElCursoNoEstaPublicado() {
        when(studentService.getById(1L)).thenReturn(new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"));
        when(courseService.getById(2L)).thenReturn(futureCourse(CourseStatus.DRAFT));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 2L))
                .isInstanceOf(CourseEnrollmentException.class)
                .hasMessage("El curso no esta publicado");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollPropagaLaExcepcionSiElAlumnoNoExiste() {
        when(studentService.getById(99L)).thenThrow(new EntityNotFoundException("Estudiante no encontrado con id 99"));

        assertThatThrownBy(() -> enrollmentService.enroll(99L, 2L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Estudiante no encontrado con id 99");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void getByIdDevuelveLaInscripcionExistente() {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        Enrollment enrollment = new Enrollment(estudiante, course, new BigDecimal("100.00"));
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThat(enrollmentService.getById(1L)).isSameAs(enrollment);
    }

    @Test
    void getByIdLanzaExceptionCuandoLaInscripcionNoExiste() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Inscripcion no encontrada con id 99");
    }

    private AuthenticatedUser administrator() {
        return new AuthenticatedUser(3L, "ada@example.com", Role.ADMINISTRATOR);
    }

    private AuthenticatedUser instructor(Long id) {
        return new AuthenticatedUser(id, "iris@example.com", Role.INSTRUCTOR);
    }

    private AuthenticatedUser student(Long id) {
        return new AuthenticatedUser(id, "ana@example.com", Role.STUDENT);
    }

    private Enrollment enrollmentOf(Long studentId, Long instructorId) {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        TestIds.setId(estudiante, studentId);
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.setId(instructor, instructorId);
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"), instructor);
        TestIds.setId(course, instructorId + 100);
        return new Enrollment(estudiante, course, new BigDecimal("100.00"));
    }

    @Test
    void getAllDeAlumnoDevuelveSoloSusInscripciones() {
        Enrollment own = enrollmentOf(9L, 4L);
        when(enrollmentRepository.findByStudentId(9L)).thenReturn(List.of(own));

        assertThat(enrollmentService.getAllByStudent(9L)).containsExactly(own);
    }

    @Test
    void getAllDeInstructorDevuelveSoloLasDeSusCursos() {
        Enrollment suya = enrollmentOf(9L, 4L);
        when(enrollmentRepository.findByCourseInstructorId(4L)).thenReturn(List.of(suya));

        assertThat(enrollmentService.getAllByInstructor(4L)).containsExactly(suya);
    }

    @Test
    void getByIdDeAlumnoConInscripcionAjenaLanzaExcepcion() {
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(enrollmentOf(7L, 4L)));

        assertThatThrownBy(() -> enrollmentService.getById(5L, student(9L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("La inscripcion no le pertenece");
    }

    @Test
    void getByIdDeAlumnoConSuInscripcionLaDevuelve() {
        Enrollment own = enrollmentOf(9L, 4L);
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(own));

        assertThat(enrollmentService.getById(5L, student(9L))).isSameAs(own);
    }

    @Test
    void getByIdDeInstructorConInscripcionDeOtroCursoLanzaExcepcion() {
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(enrollmentOf(9L, 5L)));

        assertThatThrownBy(() -> enrollmentService.getById(5L, instructor(4L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("La inscripcion no le pertenece a sus cursos");
    }

    @Test
    void getByIdDeAdministradorConInscripcionAjenaLaDevuelve() {
        Enrollment foreign = enrollmentOf(9L, 5L);
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(foreign));

        assertThat(enrollmentService.getById(5L, administrator())).isSameAs(foreign);
    }
}
