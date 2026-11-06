package com.example.demo.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.PasswordEncoder;
import com.example.demo.user.model.Student;
import com.example.demo.user.repositories.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, passwordEncoder);
    }

    @Test
    void createHasheaLaContrasenaAntesDeGuardar() {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");
        when(studentRepository.save(estudiante)).thenAnswer(invocation -> {
            assertThat(estudiante.getPassword()).isEqualTo("bcrypt-hash");
            return estudiante;
        });

        assertThat(studentService.create(estudiante).getPassword()).isEqualTo("bcrypt-hash");
        verify(studentRepository).save(estudiante);
    }

    @Test
    void updateHasheaLaNuevaContrasena() {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "nueva", "11111111");
        when(passwordEncoder.encode("nueva")).thenReturn("bcrypt-nueva");
        when(studentRepository.save(estudiante)).thenReturn(estudiante);

        assertThat(studentService.update(estudiante).getPassword()).isEqualTo("bcrypt-nueva");
    }

    @Test
    void getByIdDevuelveElEstudianteExistente() {
        Student estudiante = new Student("Ana", "Perez", "ana@example.com", "secret", "11111111");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(estudiante));

        assertThat(studentService.getById(1L)).isSameAs(estudiante);
    }

    @Test
    void getByIdLanzaExceptionCuandoElEstudianteNoExiste() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Estudiante no encontrado con id 99");
    }
}
