package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.repository.AdministratorRepository;
import java.util.List;
import java.util.Objects;
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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ADMINISTRATOR_NOT_FOUND, id));
    }

    @Transactional(readOnly = true)
    public Administrator findByEmail(String email) {
        return administratorRepository.findByEmail(email);
    }

    // TP6: un administrador no puede borrarse a si mismo (409); uno que no existe es 404
    public void delete(Long id, AuthenticatedUser actor) {
        if (actor != null && Objects.equals(id, actor.id())) {
            throw new BusinessRuleException(ErrorCode.ADMINISTRATOR_SELF_DELETION);
        }
        getById(id);
        administratorRepository.deleteById(id);
    }

    // TP6: el alta de un administrador con un email que ya usa otro responde 409
    @Override
    protected void validateEmailAvailable(String email, Long administratorId) {
        Administrator existing = administratorRepository.findByEmail(email);
        if (existing != null && (administratorId == null || !Objects.equals(existing.getId(), administratorId))) {
            throw new BusinessRuleException(ErrorCode.ADMINISTRATOR_EMAIL_TAKEN, email);
        }
    }
}
