package com.example.demo.user.services;

import com.example.demo.common.PasswordEncoder;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.repositories.InstructorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final PasswordEncoder passwordEncoder;

    public InstructorService(InstructorRepository instructorRepository, PasswordEncoder passwordEncoder) {
        this.instructorRepository = instructorRepository;
        this.passwordEncoder = passwordEncoder;
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

    public Instructor save(Instructor instructor) {
        return instructorRepository.save(instructor);
    }

    public Instructor create(Instructor instructor) {
        Objects.requireNonNull(instructor, "El instructor no puede ser nulo");
        validarEmailDisponible(instructor.getEmail(), null);
        instructor.setPassword(passwordEncoder.encode(instructor.getPassword()));
        return instructorRepository.save(instructor);
    }

    public Instructor update(Instructor instructor) {
        Objects.requireNonNull(instructor, "El instructor no puede ser nulo");
        validarEmailDisponible(instructor.getEmail(), instructor.getId());
        instructor.setPassword(passwordEncoder.encode(instructor.getPassword()));
        return instructorRepository.save(instructor);
    }

    public void delete(Long id) {
        instructorRepository.deleteById(id);
    }

    private void validarEmailDisponible(String email, Long instructorId) {
        Instructor existente = instructorRepository.findByEmail(email);
        if (existente != null && (instructorId == null || !Objects.equals(existente.getId(), instructorId))) {
            throw new InstructorEmailAlreadyExistsException(email);
        }
    }
}
