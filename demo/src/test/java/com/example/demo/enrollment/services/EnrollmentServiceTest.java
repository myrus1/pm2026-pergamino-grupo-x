package com.example.demo.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.CourseService;
import com.example.demo.enrollment.model.CourseEnrollmentException;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.repositories.EnrollmentRepository;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import com.example.demo.user.services.StudentService;
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
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, studentService, courseService);
    }

    private Course cursoFuturo(CourseStatus estado) {
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.now().plusWeeks(2),
                LocalDate.now().plusWeeks(6), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        curso.setStatus(estado);
        return curso;
    }

    @Test
    void enrollAplicaLasReglasDelAlumnoYGuardaLaInscripcion() {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        Course curso = cursoFuturo(CourseStatus.PUBLISHED);
        when(studentService.getById(1L)).thenReturn(estudiante);
        when(courseService.getById(2L)).thenReturn(curso);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment inscripcion = enrollmentService.enroll(1L, 2L);

        assertThat(inscripcion.getStudent()).isSameAs(estudiante);
        assertThat(inscripcion.getCourse()).isSameAs(curso);
        assertThat(inscripcion.getFinalAmount()).isEqualByComparingTo("100.00");
        verify(enrollmentRepository).save(inscripcion);
    }

    @Test
    void enrollNoGuardaNadaSiElCursoNoEstaPublicado() {
        when(studentService.getById(1L)).thenReturn(new Student("Ana", "Perez", "ana@example.com", "secret", "11111111"));
        when(courseService.getById(2L)).thenReturn(cursoFuturo(CourseStatus.DRAFT));

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
        Course curso = new Course("Curso", "Desc", new BigDecimal("100.00"), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20), 5, new Specialty("Programacion"),
                new Instructor("Iris", "Ruiz", "iris@example.com", "secret"));
        Enrollment inscripcion = new Enrollment(estudiante, curso, new BigDecimal("100.00"));
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(inscripcion));

        assertThat(enrollmentService.getById(1L)).isSameAs(inscripcion);
    }

    @Test
    void getByIdLanzaExceptionCuandoLaInscripcionNoExiste() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Inscripcion no encontrada con id 99");
    }
}
