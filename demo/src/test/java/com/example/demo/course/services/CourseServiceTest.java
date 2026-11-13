package com.example.demo.course.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStateException;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.repositories.CourseRepository;
import com.example.demo.testutil.TestIds;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
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

    private CourseRequest solicitud(Long specialtyId, Long instructorId) {
        CourseRequest solicitud = new CourseRequest();
        solicitud.setTitle("Java");
        solicitud.setDescription("Desc");
        solicitud.setBasePrice(new BigDecimal("100.00"));
        solicitud.setStartDate(LocalDate.of(2026, 6, 1));
        solicitud.setEndDate(LocalDate.of(2026, 6, 20));
        solicitud.setMaxCapacity(5);
        solicitud.setSpecialtyId(specialtyId);
        solicitud.setInstructorId(instructorId);
        return solicitud;
    }

    @Test
    void createArmaElCursoConEspecialidadEInstructorYLoGuarda() {
        Specialty especialidad = new Specialty("Programacion");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        instructor.addSpecialty(especialidad);
        when(specialtyService.getById(1L)).thenReturn(especialidad);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course curso = courseService.create(solicitud(1L, 2L), administrador());

        assertThat(curso.getTitle()).isEqualTo("Java");
        assertThat(curso.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(curso.getSpecialty()).isSameAs(especialidad);
        assertThat(curso.getInstructor()).isSameAs(instructor);
        assertThat(especialidad.getCourses()).containsExactly(curso);
        assertThat(instructor.getCourses()).containsExactly(curso);
    }

    @Test
    void createConEspecialidadInexistenteNoGuardaNada() {
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        assertThatThrownBy(() -> courseService.create(solicitud(99L, 2L), administrador()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createConUnaEspecialidadQueElInstructorNoTieneNoGuardaNada() {
        when(specialtyService.getById(1L)).thenReturn(new Specialty("Marketing"));
        when(instructorService.getById(2L)).thenReturn(new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));

        assertThatThrownBy(() -> courseService.create(solicitud(1L, 2L), administrador()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instructor debe tener la especialidad del curso");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCambiaLosDatosYMueveElCursoALaNuevaEspecialidad() {
        Specialty anterior = new Specialty("Diseno");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Course curso = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, anterior, instructor);
        Specialty nueva = new Specialty("Programacion");
        instructor.addSpecialty(nueva);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));
        when(specialtyService.getById(1L)).thenReturn(nueva);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(curso)).thenReturn(curso);

        Course actualizado = courseService.update(7L, solicitud(1L, 2L), administrador());

        assertThat(actualizado.getTitle()).isEqualTo("Java");
        assertThat(actualizado.getBasePrice()).isEqualByComparingTo("100.00");
        assertThat(actualizado.getSpecialty()).isSameAs(nueva);
        assertThat(anterior.getCourses()).isEmpty();
        assertThat(nueva.getCourses()).containsExactly(curso);
    }

    @Test
    void updateNoCambiaElEstadoDelCurso() {
        Specialty especialidad = new Specialty("Programacion");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        instructor.addSpecialty(especialidad);
        Course curso = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, especialidad, instructor);
        curso.setStatus(CourseStatus.PENDING_APPROVAL);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));
        when(specialtyService.getById(1L)).thenReturn(especialidad);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(curso)).thenReturn(curso);

        assertThat(courseService.update(7L, solicitud(1L, 2L), administrador()).getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void updateAUnaEspecialidadQueElInstructorNoTieneNoModificaElCurso() {
        Specialty especialidad = new Specialty("Programacion");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Course curso = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, especialidad, instructor);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));
        when(specialtyService.getById(1L)).thenReturn(new Specialty("Marketing"));
        when(instructorService.getById(2L)).thenReturn(instructor);

        assertThatThrownBy(() -> courseService.update(7L, solicitud(1L, 2L), administrador()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instructor debe tener la especialidad del curso");
        assertThat(curso.getTitle()).isEqualTo("Viejo");
        assertThat(curso.getSpecialty()).isSameAs(especialidad);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateDeCursoInexistenteLanzaExcepcion() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(99L, solicitud(1L, 2L), administrador()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Curso no encontrado con id 99");
    }

    @Test
    void getCoursesByInstructorIdDevuelveLosCursosDelInstructor() {
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        when(courseRepository.findByInstructorId(4L)).thenReturn(List.of(curso));

        assertThat(courseService.getCoursesByInstructorId(4L)).containsExactly(curso);
    }

    @Test
    void getByIdDevuelveElCursoExistente() {
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(curso));

        assertThat(courseService.getById(1L)).isSameAs(curso);
    }

    @Test
    void getByIdLanzaExceptionCuandoElCursoNoExiste() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Curso no encontrado con id 99");
    }

    private UsuarioAutenticado administrador() {
        return new UsuarioAutenticado(3L, "ada@example.com", Rol.ADMINISTRADOR);
    }

    private UsuarioAutenticado instructor(Long id) {
        return new UsuarioAutenticado(id, "iris@example.com", Rol.INSTRUCTOR);
    }

    private Instructor instructorEntidad(Long id) {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.fijarId(instructor, id);
        return instructor;
    }

    private Course cursoDe(Instructor instructor) {
        Specialty especialidad = new Specialty("Programacion");
        instructor.addSpecialty(especialidad);
        Course curso = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, especialidad, instructor);
        TestIds.fijarId(curso, 7L);
        return curso;
    }

    @Test
    void createComoInstructorUsaElInstructorDelTokenYNoElDelBody() {
        Specialty especialidad = new Specialty("Programacion");
        Instructor propio = instructorEntidad(2L);
        propio.addSpecialty(especialidad);
        when(specialtyService.getById(1L)).thenReturn(especialidad);
        when(instructorService.getById(2L)).thenReturn(propio);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course curso = courseService.create(solicitud(1L, 99L), instructor(2L));

        assertThat(curso.getInstructor()).isSameAs(propio);
        verify(instructorService, never()).getById(99L);
    }

    @Test
    void updateComoInstructorDeCursoAjenoNoModificaNada() {
        Course curso = cursoDe(instructorEntidad(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> courseService.update(7L, solicitud(1L, 2L), instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El curso pertenece a otro instructor");
        assertThat(curso.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateComoInstructorDeCursoPublicadoNoPermitido() {
        Course curso = cursoDe(instructorEntidad(2L));
        curso.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> courseService.update(7L, solicitud(1L, 2L), instructor(2L)))
                .isInstanceOf(CourseStateException.class)
                .hasMessage("El curso solo se puede modificar mientras este en borrador");
        assertThat(curso.getTitle()).isEqualTo("Viejo");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateComoAdministradorDeCursoPublicadoEstaPermitido() {
        Course curso = cursoDe(instructorEntidad(5L));
        curso.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));
        when(specialtyService.getById(1L)).thenReturn(curso.getSpecialty());
        when(instructorService.getById(5L)).thenReturn(curso.getInstructor());
        when(courseRepository.save(curso)).thenReturn(curso);

        assertThat(courseService.update(7L, solicitud(1L, 5L), administrador()).getTitle()).isEqualTo("Java");
    }

    @Test
    void deleteComoInstructorDeCursoAjenoNoBorra() {
        Course curso = cursoDe(instructorEntidad(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> courseService.delete(7L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El curso pertenece a otro instructor");
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void deleteComoInstructorDeCursoPublicadoNoBorra() {
        Course curso = cursoDe(instructorEntidad(2L));
        curso.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> courseService.delete(7L, instructor(2L)))
                .isInstanceOf(CourseStateException.class);
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void deleteComoAdministradorDeCursoPublicadoSiBorra() {
        Course curso = cursoDe(instructorEntidad(5L));
        curso.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        courseService.delete(7L, administrador());

        verify(courseRepository).deleteById(7L);
    }

    @Test
    void getByIdComoInstructorDeCursoAjenoLanzaExcepcion() {
        Course curso = cursoDe(instructorEntidad(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> courseService.getById(7L, instructor(2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessage("El curso pertenece a otro instructor");
    }

    @Test
    void getByIdComoAdministradorDeCursoAjenoDevuelveElCurso() {
        Course curso = cursoDe(instructorEntidad(5L));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));

        assertThat(courseService.getById(7L, administrador())).isSameAs(curso);
    }
}
