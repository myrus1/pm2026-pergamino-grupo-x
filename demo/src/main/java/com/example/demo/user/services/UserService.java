package com.example.demo.user.services;

import com.example.demo.common.PasswordEncoder;
import com.example.demo.user.model.User;
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
        validarEmailDisponible(user.getEmail(), null);
        return guardarConPasswordHasheada(user);
    }

    public T update(T user) {
        Objects.requireNonNull(user, "El usuario no puede ser nulo");
        validarEmailDisponible(user.getEmail(), user.getId());
        return guardarConPasswordHasheada(user);
    }

    protected void validarEmailDisponible(String email, Long userId) {
    }

    private T guardarConPasswordHasheada(T user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }
}
