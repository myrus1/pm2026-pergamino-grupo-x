package ar.edu.unnoba.pm2026.course.service;

import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.repository.SpecialtyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional(readOnly = true)
    public List<Specialty> getAll() {
        return specialtyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Specialty getById(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada con id " + id));
    }

    public Specialty save(Specialty specialty) {
        return specialtyRepository.save(specialty);
    }

    public void delete(Long id) {
        specialtyRepository.deleteById(id);
    }
}
