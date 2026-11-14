package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.SpecialtyService;
import ar.edu.unnoba.pm2026.testutil.TestIds;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.repository.InstructorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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

    @Mock
    private SpecialtyService specialtyService;

    private InstructorService instructorService;

    @BeforeEach
    void setUp() {
        instructorService = new InstructorService(instructorRepository, passwordEncoder, specialtyService);
    }

    @Test
    void getSpecialtiesDevuelveLosDtoDelInstructor() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Specialty programming = new Specialty("Programacion");
        Specialty diseno = new Specialty("Diseno");
        TestIds.setId(programming, 4L);
        TestIds.setId(diseno, 5L);
        instructor.addSpecialty(programming);
        instructor.addSpecialty(diseno);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        List<SpecialtyResponse> result = instructorService.getSpecialties(1L);

        assertThat(result).extracting("id").containsExactlyInAnyOrder(4L, 5L);
        assertThat(result).extracting("name").containsExactlyInAnyOrder("Programacion", "Diseno");
    }

    @Test
    void getSpecialtiesDeInstructorInexistenteLanzaNotFound() {
        when(instructorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructorService.getSpecialties(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Instructor no encontrado con id 99");
    }

    @Test
    void findByEmailUsaElRepositorio() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        when(instructorRepository.findByEmail("iris@example.com")).thenReturn(instructor);

        assertThat(instructorService.findByEmail("iris@example.com")).isSameAs(instructor);
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

    @Test
    void updateConSuPropioEmailHasheaYGuarda() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "nueva");
        TestIds.setId(instructor, 5L);
        when(instructorRepository.findByEmail("iris@example.com")).thenReturn(instructor);
        when(passwordEncoder.encode("nueva")).thenReturn("bcrypt-nueva");
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        assertThat(instructorService.update(instructor).getPassword()).isEqualTo("bcrypt-nueva");
    }

    @Test
    void addSpecialtyVinculaLaEspecialidadDeLosDosLados() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Specialty specialty = new Specialty("Programacion");
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(specialtyService.getById(3L)).thenReturn(specialty);

        instructorService.addSpecialty(1L, 3L);

        assertThat(instructor.getSpecialties()).containsExactly(specialty);
        assertThat(specialty.getInstructors()).containsExactly(instructor);
        verify(instructorRepository).save(instructor);
    }

    @Test
    void removeSpecialtyDesvinculaLaEspecialidadDeLosDosLados() {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        Specialty specialty = new Specialty("Programacion");
        instructor.addSpecialty(specialty);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(specialtyService.getById(3L)).thenReturn(specialty);

        instructorService.removeSpecialty(1L, 3L);

        assertThat(instructor.getSpecialties()).isEmpty();
        assertThat(specialty.getInstructors()).isEmpty();
        verify(instructorRepository).save(instructor);
    }

    @Test
    void addSpecialtyConEspecialidadInexistenteNoGuardaNada() {
        when(instructorRepository.findById(1L))
                .thenReturn(Optional.of(new Instructor("Iris", "Ruiz", "iris@example.com", "secret")));
        when(specialtyService.getById(99L)).thenThrow(new EntityNotFoundException("Especialidad no encontrada con id 99"));

        assertThatThrownBy(() -> instructorService.addSpecialty(1L, 99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Especialidad no encontrada con id 99");
        verify(instructorRepository, never()).save(any());
    }
}
