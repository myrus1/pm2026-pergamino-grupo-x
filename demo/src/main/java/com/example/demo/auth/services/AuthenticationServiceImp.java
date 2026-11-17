package com.example.demo.auth.services;

import com.example.demo.auth.dto.AuthenticationResult;
import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;
import com.example.demo.common.Rol;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import com.example.demo.user.model.User;
import com.example.demo.user.repositories.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImp implements AuthenticationService {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationServiceImp(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public AuthenticationResult authenticate(String email, String password) {
        User registrado = buscarPorEmail(email);
        if (registrado == null || !passwordValida(password, registrado.getPassword())) {
            throw new AuthenticationException("Email o contraseña incorrectos");
        }
        String token = jwtTokenUtil.generateToken(registrado.getEmail(), registrado.getRol(), registrado.getId());
        return new AuthenticationResult(token, registrado.getRol(), jwtTokenUtil.getExpirationSeconds());
    }

    @Override
    public String authenticate(Instructor instructor) {
        return autenticarConRol(instructor.getEmail(), instructor.getPassword(), Rol.INSTRUCTOR);
    }

    @Override
    public String authenticate(Student student) {
        return autenticarConRol(student.getEmail(), student.getPassword(), Rol.STUDENT);
    }

    @Override
    public String authenticate(Administrator administrator) {
        return autenticarConRol(administrator.getEmail(), administrator.getPassword(), Rol.ADMINISTRATOR);
    }

    private String autenticarConRol(String email, String password, Rol rolEsperado) {
        AuthenticationResult resultado = authenticate(email, password);
        if (resultado.rol() != rolEsperado) {
            // Mantiene el comportamiento de los logins viejos: cada uno solo acepta su rol.
            throw new AuthenticationException("Email o contraseña incorrectos");
        }
        return PREFIJO_BEARER + resultado.token();
    }

    private User buscarPorEmail(String email) {
        if (email == null) {
            return null;
        }
        Optional<User> registrado = userRepository.findByEmail(email);
        return registrado == null ? null : registrado.orElse(null);
    }

    // Password4j lanza IllegalArgumentException si la contraseña guardada no es un hash BCrypt
    private boolean passwordValida(String password, String hashGuardado) {
        try {
            return passwordEncoder.verify(password, hashGuardado);
        } catch (IllegalArgumentException | NullPointerException excepcion) {
            return false;
        }
    }
}
