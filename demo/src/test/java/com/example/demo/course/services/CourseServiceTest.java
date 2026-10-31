package com.example.demo.course.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.repositories.CourseRepository;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        when(specialtyService.getById(1L)).thenReturn(especialidad);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course curso = courseService.create(solicitud(1L, 2L));

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

        assertThatThrownBy(() -> courseService.create(solicitud(99L, 2L)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCambiaLosDatosYMueveElCursoALaNuevaEspecialidad() {
        Specialty anterior = new Specialty("Diseno");
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Course curso = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, anterior, instructor);
        Specialty nueva = new Specialty("Programacion");
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));
        when(specialtyService.getById(1L)).thenReturn(nueva);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(curso)).thenReturn(curso);

        Course actualizado = courseService.update(7L, solicitud(1L, 2L));

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
        Course curso = new Course("Viejo", "Desc", new BigDecimal("50.00"), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20), 3, especialidad, instructor);
        curso.setStatus(CourseStatus.PENDING_APPROVAL);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(curso));
        when(specialtyService.getById(1L)).thenReturn(especialidad);
        when(instructorService.getById(2L)).thenReturn(instructor);
        when(courseRepository.save(curso)).thenReturn(curso);

        assertThat(courseService.update(7L, solicitud(1L, 2L)).getStatus()).isEqualTo(CourseStatus.PENDING_APPROVAL);
    }

    @Test
    void updateDeCursoInexistenteLanzaExcepcion() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(99L, solicitud(1L, 2L)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Curso no encontrado con id 99");
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
}
