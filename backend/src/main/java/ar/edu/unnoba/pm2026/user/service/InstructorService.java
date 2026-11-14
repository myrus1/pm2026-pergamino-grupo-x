package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.SpecialtyService;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.repository.InstructorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstructorService extends UserService<Instructor> {

    private final InstructorRepository instructorRepository;
    private final SpecialtyService specialtyService;

    public InstructorService(InstructorRepository instructorRepository, PasswordEncoder passwordEncoder,
            SpecialtyService specialtyService) {
        super(instructorRepository, passwordEncoder);
        this.instructorRepository = instructorRepository;
        this.specialtyService = specialtyService;
    }

    @Transactional(readOnly = true)
    public List<Instructor> getAll() {
        return instructorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Instructor getById(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Instructor no encontrado con id " + id));
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> getSpecialties(Long instructorId) {
        return getById(instructorId).getSpecialties().stream()
                .map(specialty -> new SpecialtyResponse(specialty.getId(), specialty.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Instructor findByEmail(String email) {
        return instructorRepository.findByEmail(email);
    }

    public void delete(Long id) {
        instructorRepository.deleteById(id);
    }

    public void addSpecialty(Long instructorId, Long specialtyId) {
        Instructor instructor = getById(instructorId);
        Specialty specialty = specialtyService.getById(specialtyId);
        instructor.addSpecialty(specialty);
        instructorRepository.save(instructor);
    }

    public void removeSpecialty(Long instructorId, Long specialtyId) {
        Instructor instructor = getById(instructorId);
        Specialty specialty = specialtyService.getById(specialtyId);
        instructor.removeSpecialty(specialty);
        instructorRepository.save(instructor);
    }

    @Override
    protected void validateEmailAvailable(String email, Long instructorId) {
        Instructor existente = instructorRepository.findByEmail(email);
        if (existente != null && (instructorId == null || !Objects.equals(existente.getId(), instructorId))) {
            throw new InstructorEmailAlreadyExistsException(email);
        }
    }
}
