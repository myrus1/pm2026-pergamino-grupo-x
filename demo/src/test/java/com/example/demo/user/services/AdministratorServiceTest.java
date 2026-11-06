package com.example.demo.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.PasswordEncoder;
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

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdministratorService administratorService;

    @BeforeEach
    void setUp() {
        administratorService = new AdministratorService(administratorRepository, passwordEncoder);
    }

    @Test
    void createHasheaLaContrasenaAntesDeGuardar() {
        Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "secret");
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");
        when(administratorRepository.save(admin)).thenAnswer(invocation -> {
            assertThat(admin.getPassword()).isEqualTo("bcrypt-hash");
            return admin;
        });

        assertThat(administratorService.create(admin).getPassword()).isEqualTo("bcrypt-hash");
        verify(administratorRepository).save(admin);
    }

    @Test
    void updateHasheaLaNuevaContrasena() {
        Administrator admin = new Administrator("Ada", "Lopez", "ada@example.com", "nueva");
        when(passwordEncoder.encode("nueva")).thenReturn("bcrypt-nueva");
        when(administratorRepository.save(admin)).thenReturn(admin);

        assertThat(administratorService.update(admin).getPassword()).isEqualTo("bcrypt-nueva");
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
