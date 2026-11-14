package ar.edu.unnoba.pm2026.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.repository.SpecialtyRepository;
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
        Specialty specialty = new Specialty("Programacion");
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));

        assertThat(specialtyService.getById(1L)).isSameAs(specialty);
    }

    @Test
    void getByIdLanzaExceptionCuandoLaEspecialidadNoExiste() {
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Especialidad no encontrada con id 99");
    }
}
