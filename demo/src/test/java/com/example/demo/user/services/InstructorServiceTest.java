package com.example.demo.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.user.model.Instructor;
import com.example.demo.user.repositories.InstructorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstructorServiceTest {

    @Mock
    private InstructorRepository instructorRepository;

    private InstructorService instructorService;

    @BeforeEach
    void setUp() {
        instructorService = new InstructorService(instructorRepository);
    }

    @Test
    void getByIdDevuelveElInstructorExistente() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        assertThat(instructorService.getById(1L)).isSameAs(instructor);
    }

    @Test
    void getByIdLanzaExceptionCuandoElInstructorNoExiste() {
        when(instructorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructorService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Instructor no encontrado con id 99");
    }
}
