package com.example.demo.course.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.repositories.CourseRepository;
import com.example.demo.user.model.Instructor;
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

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository);
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
