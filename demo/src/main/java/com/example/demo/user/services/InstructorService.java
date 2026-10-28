package com.example.demo.user.services;

import com.example.demo.user.model.Instructor;
import com.example.demo.user.repositories.InstructorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstructorService {

    private final InstructorRepository instructorRepository;

    public InstructorService(InstructorRepository instructorRepository) {
        this.instructorRepository = instructorRepository;
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

    public void delete(Long id) {
        instructorRepository.deleteById(id);
    }
}
