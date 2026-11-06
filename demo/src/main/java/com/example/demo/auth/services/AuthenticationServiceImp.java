package com.example.demo.auth.services;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImp implements AuthenticationService {

    private final InstructorService instructorService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationServiceImp(InstructorService instructorService, PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.instructorService = instructorService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public String authenticate(Instructor instructor) throws Exception {
        Instructor registrado = instructorService.findByEmail(instructor.getEmail());
        if (registrado == null || !passwordValida(instructor.getPassword(), registrado.getPassword())) {
            throw new AuthenticationException("Email o contraseña incorrectos");
        }
        return jwtTokenUtil.generateToken(registrado.getEmail());
    }

    // Password4j lanza IllegalArgumentException si la contraseña guardada no es un hash BCrypt
    private boolean passwordValida(String password, String hashGuardado) {
        try {
            return passwordEncoder.verify(password, hashGuardado);
        } catch (IllegalArgumentException excepcion) {
            return false;
        }
    }
}
