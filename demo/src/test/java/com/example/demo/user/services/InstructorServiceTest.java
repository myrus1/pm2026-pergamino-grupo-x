package com.example.demo.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.PasswordEncoder;
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

    @Mock
    private PasswordEncoder passwordEncoder;

    private InstructorService instructorService;

    @BeforeEach
    void setUp() {
        instructorService = new InstructorService(instructorRepository, passwordEncoder);
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

    @Test
    void createCodificaLaContrasenaAntesDeGuardar() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        when(instructorRepository.findByEmail("iris@example.com")).thenReturn(null);
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        Instructor created = instructorService.create(instructor);

        assertThat(created).isSameAs(instructor);
        assertThat(created.getPassword()).isEqualTo("bcrypt-hash");
        verify(instructorRepository).save(instructor);
    }

    @Test
    void createRechazaEmailExistenteSinCodificarNiGuardar() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        when(instructorRepository.findByEmail("iris@example.com"))
                .thenReturn(new Instructor("Otra", "Persona", "iris@example.com", "existing-hash"));

        assertThatThrownBy(() -> instructorService.create(instructor))
                .isInstanceOf(InstructorEmailAlreadyExistsException.class)
                .hasMessage("Ya existe un instructor con el email iris@example.com");

        verify(passwordEncoder, never()).encode(any());
        verify(instructorRepository, never()).save(any());
    }
}
