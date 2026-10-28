package com.example.demo.enrollment.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.repositories.EnrollmentRepository;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
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

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository);
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
