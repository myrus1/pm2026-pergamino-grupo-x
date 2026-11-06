package com.example.demo.user.services;

import com.example.demo.common.PasswordEncoder;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.SpecialtyService;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.repositories.InstructorRepository;
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
    protected void validarEmailDisponible(String email, Long instructorId) {
        Instructor existente = instructorRepository.findByEmail(email);
        if (existente != null && (instructorId == null || !Objects.equals(existente.getId(), instructorId))) {
            throw new InstructorEmailAlreadyExistsException(email);
        }
    }
}
