package com.example.demo.course.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.course.model.Specialty;
import com.example.demo.course.repositories.SpecialtyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    private SpecialtyService specialtyService;

    @BeforeEach
    void setUp() {
        specialtyService = new SpecialtyService(specialtyRepository);
    }

    @Test
    void getByIdDevuelveLaEspecialidadExistente() {
        Specialty especialidad = new Specialty("Programacion");
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(especialidad));

        assertThat(specialtyService.getById(1L)).isSameAs(especialidad);
    }

    @Test
    void getByIdLanzaExceptionCuandoLaEspecialidadNoExiste() {
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Especialidad no encontrada con id 99");
    }
}
