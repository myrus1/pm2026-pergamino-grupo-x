package ar.edu.unnoba.pm2026.course.service;

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

    private CourseRequest request(Long specialtyId, Long instructorId) {
        CourseRequest request = new CourseRequest();
        request.setTitle("Java");
        request.setDescription("Desc");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 20));
        request.setMaxCapacity(5);
        request.setSpecialtyId(specialtyId);
        request.setInstructorId(instructorId);
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

        Course course = courseService.create(request(1L, 2L), administrator());

        assertThat(course.getTitle()).isEqualTo("Java");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.getSpecialty()).isSameAs(specialty);
        assertThat(course.getInstructor()).isSameAs(instructor);
        assertThat(specialty.getCourses()).containsExactly(course);
        assertThat(instructor.getCourses()).containsExactly(course);
    }

    @Test
    void createConEspecialidadInexistenteNoGuardaNada() {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        assertThatThrownBy(() -> courseService.create(request(99L, 2L), administrator()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createConUnaEspecialidadQueElInstructorNoTieneNoGuardaNada() {
        when(specialtyService.getById(1L)).thenReturn(new Specialty("Marketing"));
        when(instructorService.getById(2L)).thenReturn(new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));

        assertThatThrownBy(() -> courseService.create(request(1L, 2L), administrator()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instructor debe tener la especialidad del curso");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCambiaLosDatosYMueveElCursoALaNuevaEspecialidad() {
        Specialty anterior = new Specialty("Diseno");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Course course = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, anterior, instructor);
        Specialty nueva = new Specialty("Programacion");
        instructor.addSpecialty(nueva);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(nueva);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(course)).thenReturn(course);

        Course updated = courseService.update(7L, request(1L, 2L), administrator());

        assertThat(updated.getTitle()).isEqualTo("Java");
        assertThat(updated.getBasePrice()).isEqualByComparingTo("100.00");
        assertThat(updated.getSpecialty()).isSameAs(nueva);
        assertThat(anterior.getCourses()).isEmpty();
        assertThat(nueva.getCourses()).containsExactly(course);
    }

    @Test
    void updateNoCambiaElEstadoDelCurso() {
        Specialty specialty = new Specialty("Programacion");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        instructor.addSpecialty(specialty);
        Course course = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, specialty, instructor);
        course.setStatus(CourseStatus.PENDING_APPROVAL);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(specialty);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(course)).thenReturn(course);

        assertThat(courseService.update(7L, request(1L, 2L), administrator()).getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void updateAUnaEspecialidadQueElInstructorNoTieneNoModificaElCurso() {
        Specialty specialty = new Specialty("Programacion");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Course course = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, specialty, instructor);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(new Specialty("Marketing"));
        when(instructorService.getById(2L)).thenReturn(instructor);

        assertThatThrownBy(() -> courseService.update(7L, request(1L, 2L), administrator()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instructor debe tener la especialidad del curso");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        assertThat(course.getSpecialty()).isSameAs(specialty);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateDeCursoInexistenteLanzaExcepcion() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(99L, request(1L, 2L), administrator()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Curso no encontrado con id 99");
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
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Curso no encontrado con id 99");
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
    void createComoInstructorUsaElInstructorDelTokenYNoElDelBody() {
        Specialty specialty = new Specialty("Programacion");
        Instructor own = instructorEntity(2L);
        own.addSpecialty(specialty);
        when(specialtyService.getById(1L)).thenReturn(specialty);
        when(instructorService.getById(2L)).thenReturn(own);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course course = courseService.create(request(1L, 99L), instructor(2L));

        assertThat(course.getInstructor()).isSameAs(own);
        verify(instructorService, never()).getById(99L);
    }

    @Test
    void updateComoInstructorDeCursoAjenoNoModificaNada() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.update(7L, request(1L, 2L), instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El curso pertenece a otro instructor");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateComoInstructorDeCursoPublicadoNoPermitido() {
        Course course = courseOf(instructorEntity(2L));
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.update(7L, request(1L, 2L), instructor(2L)))
                .isInstanceOf(CourseStateException.class)
                .hasMessage("El curso solo se puede modificar mientras este en borrador");
        assertThat(course.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateComoAdministradorDeCursoPublicadoEstaPermitido() {
        Course course = courseOf(instructorEntity(5L));
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(specialtyService.getById(1L)).thenReturn(course.getSpecialty());
        when(instructorService.getById(5L)).thenReturn(course.getInstructor());
        when(courseRepository.save(course)).thenReturn(course);

        assertThat(courseService.update(7L, request(1L, 5L), administrator()).getTitle()).isEqualTo("Java");
    }

    @Test
    void deleteComoInstructorDeCursoAjenoNoBorra() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.delete(7L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El curso pertenece a otro instructor");
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
                .hasMessage("El curso pertenece a otro instructor");
    }

    @Test
    void getByIdComoAdministradorDeCursoAjenoDevuelveElCurso() {
        Course course = courseOf(instructorEntity(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThat(courseService.getById(7L, administrator())).isSameAs(course);
    }
}
