package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.user.model.User;
import java.util.Objects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class UserService<T extends User> {

    private final JpaRepository<T, Long> repository;
    private final PasswordEncoder passwordEncoder;

    protected UserService(JpaRepository<T, Long> repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public T create(T user) {
        Objects.requireNonNull(user, "El usuario no puede ser nulo");
        validateEmailAvailable(user.getEmail(), null);
        return saveWithHashedPassword(user);
    }

    public T update(T user) {
        Objects.requireNonNull(user, "El usuario no puede ser nulo");
        validateEmailAvailable(user.getEmail(), user.getId());
        return saveWithHashedPassword(user);
    }

    protected void validateEmailAvailable(String email, Long userId) {
    }

    private T saveWithHashedPassword(T user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }
}
