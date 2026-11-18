package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.repository.AdministratorRepository;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
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
        Administrator administrator = new Administrator("Eva", "Sosa", "eva@example.com", "secret");
        when(administratorRepository.findById(1L)).thenReturn(Optional.of(administrator));

        assertThat(administratorService.getById(1L)).isSameAs(administrator);
    }

    @Test
    void getByIdLanzaExceptionCuandoElAdministradorNoExiste() {
        when(administratorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administratorService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Administrador no encontrado con id 99");
    }

    @Test
    void findByEmailDevuelveElAdministradorExistente() {
        Administrator administrator = new Administrator("Ada", "Lopez", "ada@example.com", "secret");
        when(administratorRepository.findByEmail("ada@example.com")).thenReturn(administrator);

        assertThat(administratorService.findByEmail("ada@example.com")).isSameAs(administrator);
    }

    @Test
    void findByEmailDevuelveNullCuandoElAdministradorNoExiste() {
        when(administratorRepository.findByEmail("nadie@example.com")).thenReturn(null);

        assertThat(administratorService.findByEmail("nadie@example.com")).isNull();
    }

    private static final AuthenticatedUser ADA = new AuthenticatedUser(3L, "ada@example.com", Role.ADMINISTRATOR);

    // TP6: un administrador no puede borrarse a si mismo
    @Test
    void deleteDeSiMismoLanzaConflictoYNoBorra() {
        assertThatThrownBy(() -> administratorService.delete(3L, ADA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Un administrador no puede eliminarse a si mismo");
        verify(administratorRepository, never()).deleteById(any());
    }

    @Test
    void deleteDeOtroAdministradorLoBorra() {
        when(administratorRepository.findById(5L)).thenReturn(Optional.of(Fixtures.administrator(5L)));

        administratorService.delete(5L, ADA);

        verify(administratorRepository).deleteById(5L);
    }

    @Test
    void deleteDeAdministradorInexistenteLanzaNotFound() {
        when(administratorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administratorService.delete(99L, ADA))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(administratorRepository, never()).deleteById(any());
    }

    @Test
    void createConEmailDeOtroAdministradorLanzaConflictoYNoGuarda() {
        when(administratorRepository.findByEmail("eva@example.com")).thenReturn(Fixtures.administrator(5L));

        assertThatThrownBy(() -> administratorService.create(Fixtures.administrator(null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ya existe un administrador con el email eva@example.com");
        verify(administratorRepository, never()).save(any());
    }
}
