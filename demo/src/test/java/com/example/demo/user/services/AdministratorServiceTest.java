package com.example.demo.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.user.model.Administrator;
import com.example.demo.user.repositories.AdministratorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdministratorServiceTest {

    @Mock
    private AdministratorRepository administratorRepository;

    private AdministratorService administratorService;

    @BeforeEach
    void setUp() {
        administratorService = new AdministratorService(administratorRepository);
    }

    @Test
    void getByIdDevuelveElAdministradorExistente() {
        Administrator administrador = new Administrator("Eva", "Sosa", "eva@example.com", "secret");
        when(administratorRepository.findById(1L)).thenReturn(Optional.of(administrador));

        assertThat(administratorService.getById(1L)).isSameAs(administrador);
    }

    @Test
    void getByIdLanzaExceptionCuandoElAdministradorNoExiste() {
        when(administratorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administratorService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Administrador no encontrado con id 99");
    }
}
