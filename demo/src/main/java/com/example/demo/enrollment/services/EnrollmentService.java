package com.example.demo.enrollment.services;

import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.repositories.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getAll() {
        return enrollmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Enrollment getById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscripcion no encontrada con id " + id));
    }

    public Enrollment save(Enrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    public void delete(Long id) {
        enrollmentRepository.deleteById(id);
    }
}
