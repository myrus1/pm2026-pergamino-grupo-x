package ar.edu.unnoba.pm2026.course.service;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

import ar.edu.unnoba.pm2026.course.dto.UpdateCourseRequest;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.exception.CourseStateException;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.repository.CourseRepository;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SpecialtyService specialtyService;

    @Mock
    private InstructorService instructorService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, specialtyService, instructorService);
    }

    private CourseRequest request(Long specialtyId) {
        CourseRequest request = new CourseRequest();
        request.setTitle("Java");
        request.setDescription("Desc");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 20));
        request.setMaxCapacity(5);
        request.setSpecialtyId(specialtyId);
        return request;
    }

    @Test
    void createArmaElCursoConEspecialidadEInstructorYLoGuarda() {
        Specialty specialty = new Specialty("Programacion");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        instructor.addSpecialty(specialty);
        when(specialtyService.getById(1L)).thenReturn(specialty);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course course = courseService.create(request(1L), instructor(2L));

        assertThat(course.getTitle()).isEqualTo("Java");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.getSpecialty()).isSameAs(specialty);
        assertThat(course.getInstructor()).isSameAs(instructor);
        assertThat(specialty.getCourses()).containsExactly(course);
        assertThat(instructor.getCourses()).containsExactly(course);
    }

    // TP6: el administrador no hace acciones de instructor
    @Test
    void createComoAdministradorLanzaAutorizacionYNoGuardaNada() {
        assertThatThrownBy(() -> courseService.create(request(1L), administrator()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("authorization.instructor-only");
        verify(specialtyService, never()).getById(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createConEspecialidadInexistenteNoGuardaNada() {
        when(specialtyService.getById(99L)).thenThrow(new ResourceNotFoundException(ErrorCode.SPECIALTY_NOT_FOUND, 99L));

        assertThatThrownBy(() -> courseService.create(request(99L), instructor(2L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createConUnaEspecialidadQueElInstructorNoTieneNoGuardaNada() {
        when(specialtyService.getById(1L)).thenReturn(new Specialty("Marketing"));
        when(instructorService.getById(2L)).thenReturn(new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));

        assertThatThrownBy(() -> courseService.create(request(1L), instructor(2L)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("instructor.specialty.mismatch");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCambiaLosDatosYMueveElCursoALaNuevaEspecialidad() {
        Specialty anterior = new Specialty("Diseno");
        Instructor instructor = instructorEntity(2L);
        Course course = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, anterior, instructor);
        Specialty nueva = new Specialty("Programacion");
        instructor.addSpecialty(nueva);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(nueva);
        when(courseRepository.save(course)).thenReturn(course);

        Course updated = courseService.update(7L, request(1L), instructor(2L));

        assertThat(updated.getTitle()).isEqualTo("Java");
        assertThat(updated.getBasePrice()).isEqualByComparingTo("100.00");
        assertThat(updated.getSpecialty()).isSameAs(nueva);
        assertThat(anterior.getCourses()).isEmpty();
        assertThat(nueva.getCourses()).containsExactly(course);
    }

    @Test
    void updateNoCambiaElEstadoNiElInstructorDelCurso() {
        Course course = courseOf(instructorEntity(2L));
        Instructor owner = course.getInstructor();
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(course.getSpecialty());
        when(courseRepository.save(course)).thenReturn(course);

        Course updated = courseService.update(7L, request(1L), instructor(2L));

        assertThat(updated.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(updated.getInstructor()).isSameAs(owner);
    }

    @Test
    void updateAUnaEspecialidadQueElInstructorNoTieneNoModificaElCurso() {
        Course course = courseOf(instructorEntity(2L));
        Specialty specialty = course.getSpecialty();
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(new Specialty("Marketing"));

        assertThatThrownBy(() -> courseService.update(7L, request(1L), instructor(2L)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("instructor.specialty.mismatch");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        assertThat(course.getSpecialty()).isSameAs(specialty);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateDeCursoInexistenteLanzaExcepcion() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(99L, request(1L), instructor(2L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("course.not-found");
    }

    @Test
    void getCoursesByInstructorIdDevuelveLosCursosDelInstructor() {
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        when(courseRepository.findByInstructorId(4L)).thenReturn(List.of(course));

        assertThat(courseService.getCoursesByInstructorId(4L)).containsExactly(course);
    }

    @Test
    void getByIdDevuelveElCursoExistente() {
        Course course = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThat(courseService.getById(1L)).isSameAs(course);
    }

    @Test
    void getByIdLanzaExceptionCuandoElCursoNoExiste() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("course.not-found");
    }

    private AuthenticatedUser administrator() {
        return new AuthenticatedUser(3L, "ada@example.com", Role.ADMINISTRATOR);
    }

    private AuthenticatedUser instructor(Long id) {
        return new AuthenticatedUser(id, "iris@example.com", Role.INSTRUCTOR);
    }

    private Instructor instructorEntity(Long id) {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.setId(instructor, id);
        return instructor;
    }

    private Course courseOf(Instructor instructor) {
        Specialty specialty = new Specialty("Programacion");
        instructor.addSpecialty(specialty);
        Course course = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, specialty, instructor);
        TestIds.setId(course, 7L);
        return course;
    }

    @Test
    void createComoInstructorLoCreaASuNombre() {
        Specialty specialty = new Specialty("Programacion");
        Instructor own = instructorEntity(2L);
        own.addSpecialty(specialty);
        when(specialtyService.getById(1L)).thenReturn(specialty);
        when(instructorService.getById(2L)).thenReturn(own);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course course = courseService.create(request(1L), instructor(2L));

        assertThat(course.getInstructor()).isSameAs(own);
    }

    @Test
    void updateComoInstructorDeCursoAjenoNoModificaNada() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.update(7L, request(1L), instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("authorization.course-not-owner");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateComoInstructorDeCursoPublicadoNoPermitido() {
        Course course = courseOf(instructorEntity(2L));
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.update(7L, request(1L), instructor(2L)))
                .isInstanceOf(CourseStateException.class)
                .hasMessage("course.not-draft");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    // TP6: el administrador no edita cursos, ni siquiera publicados
    @Test
    void updateComoAdministradorLanzaAutorizacionYNoModificaNada() {
        assertThatThrownBy(() -> courseService.update(7L, request(1L), administrator()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("authorization.instructor-only");
        verify(courseRepository, never()).findById(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void deleteComoInstructorDeCursoAjenoNoBorra() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.delete(7L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("authorization.course-not-owner");
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void deleteComoInstructorDeCursoPublicadoNoBorra() {
        Course course = courseOf(instructorEntity(2L));
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.delete(7L, instructor(2L)))
                .isInstanceOf(CourseStateException.class);
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void deleteComoAdministradorDeCursoPublicadoSiBorra() {
        Course course = courseOf(instructorEntity(5L));
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        courseService.delete(7L, administrator());

        verify(courseRepository).deleteById(7L);
    }

    @Test
    void getByIdComoInstructorDeCursoAjenoLanzaExcepcion() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.getById(7L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("authorization.course-not-owner");
    }

    @Test
    void getByIdComoAdministradorDeCursoAjenoDevuelveElCurso() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThat(courseService.getById(7L, administrator())).isSameAs(course);
    }

    @Test
    void getDetailCuentaInscriptosYLugaresLibres() {
        Course course = Fixtures.course(7L, CourseStatus.PUBLISHED);
        new Enrollment(Fixtures.student(1L), course, new BigDecimal("100.00"));
        new Enrollment(Fixtures.student(2L), course, new BigDecimal("75.00"));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        CourseDetailResponse detail = courseService.getDetail(7L);

        assertThat(detail.enrolledStudents()).isEqualTo(2);
        assertThat(detail.availableSeats()).isEqualTo(3);
        assertThat(detail.instructor().name()).isEqualTo("Iris");
        assertThat(detail.specialty().getName()).isEqualTo("Programacion");
    }

    @Test
    void getPublishedPideLosPublicadosOrdenadosPorFecha() {
        Course course = Fixtures.course(7L, CourseStatus.PUBLISHED);
        when(courseRepository.findByStatusOrderByStartDateDesc(CourseStatus.PUBLISHED)).thenReturn(List.of(course));

        assertThat(courseService.getPublished()).containsExactly(course);
    }

    @Test
    void getByInstructorAndStatusDeInstructorInexistenteLanzaNotFound() {
        when(instructorService.getById(99L)).thenThrow(new ResourceNotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND, 99L));

        assertThatThrownBy(() -> courseService.getByInstructorAndStatus(99L, CourseStatus.PUBLISHED))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(courseRepository, never()).findByInstructorIdAndStatusOrderByStartDateDesc(any(), any());
    }

    private UpdateCourseRequest changes() {
        return new UpdateCourseRequest();
    }

    @Test
    void updatePartiallyCambiaSoloLosCamposQueVienen() {
        Course course = courseOf(instructorEntity(2L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        UpdateCourseRequest changes = changes();
        changes.setTitle("Java 2");

        Course updated = courseService.updatePartially(7L, changes, instructor(2L));

        assertThat(updated.getTitle()).isEqualTo("Java 2");
        assertThat(updated.getBasePrice()).isEqualByComparingTo("50.00");
        assertThat(updated.getMaxCapacity()).isEqualTo(3);
        assertThat(updated.getDescription()).isEqualTo("Desc");
        verify(specialtyService, never()).getById(any());
    }

    @Test
    void updatePartiallyConFechaDeFinAntesDelInicioNoModificaNada() {
        Course course = courseOf(instructorEntity(2L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        UpdateCourseRequest changes = changes();
        changes.setTitle("Java 2");
        changes.setEndDate(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> courseService.updatePartially(7L, changes, instructor(2L)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("course.end-date.before-start");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updatePartiallyConOtraEspecialidadValidaQueSeaDelInstructor() {
        Course course = courseOf(instructorEntity(2L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(9L)).thenReturn(new Specialty("Marketing"));
        UpdateCourseRequest changes = changes();
        changes.setSpecialtyId(9L);

        assertThatThrownBy(() -> courseService.updatePartially(7L, changes, instructor(2L)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("instructor.specialty.mismatch");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updatePartiallyDeCursoPublicadoLanzaConflicto() {
        Course course = courseOf(instructorEntity(2L));
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.updatePartially(7L, changes(), instructor(2L)))
                .isInstanceOf(CourseStateException.class);
    }

    @Test
    void updatePartiallyComoAdministradorLanzaAutorizacion() {
        assertThatThrownBy(() -> courseService.updatePartially(7L, changes(), administrator()))
                .isInstanceOf(AuthorizationException.class);
        verify(courseRepository, never()).findById(any());
    }

    @Test
    void getDetailComoInstructorDeCursoAjenoLanzaAutorizacion() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(courseOf(instructorEntity(5L))));

        assertThatThrownBy(() -> courseService.getDetail(7L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("authorization.course-not-owner");
    }
}
