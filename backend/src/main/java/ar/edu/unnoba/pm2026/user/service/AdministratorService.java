package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.repository.AdministratorRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdministratorService extends UserService<Administrator> {

    private final AdministratorRepository administratorRepository;

    public AdministratorService(AdministratorRepository administratorRepository, PasswordEncoder passwordEncoder) {
        super(administratorRepository, passwordEncoder);
        this.administratorRepository = administratorRepository;
    }

    @Transactional(readOnly = true)
    public List<Administrator> getAll() {
        return administratorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Administrator getById(Long id) {
        return administratorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Administrador no encontrado con id " + id));
    }

    @Transactional(readOnly = true)
    public Administrator findByEmail(String email) {
        return administratorRepository.findByEmail(email);
    }

    public void delete(Long id) {
        administratorRepository.deleteById(id);
    }
}
