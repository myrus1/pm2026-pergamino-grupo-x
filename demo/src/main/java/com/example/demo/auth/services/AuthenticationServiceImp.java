package com.example.demo.auth.services;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;
import com.example.demo.common.Rol;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;
import com.example.demo.user.model.User;
import com.example.demo.user.services.AdministratorService;
import com.example.demo.user.services.InstructorService;
import com.example.demo.user.services.StudentService;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImp implements AuthenticationService {

    private final InstructorService instructorService;
    private final StudentService studentService;
    private final AdministratorService administratorService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationServiceImp(InstructorService instructorService, StudentService studentService,
            AdministratorService administratorService, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.administratorService = administratorService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public String authenticate(Instructor instructor) {
        return autenticar(instructor, Rol.INSTRUCTOR, instructorService::findByEmail);
    }

    @Override
    public String authenticate(Student student) {
        return autenticar(student, Rol.ALUMNO, studentService::findByEmail);
    }

    @Override
    public String authenticate(Administrator administrator) {
        return autenticar(administrator, Rol.ADMINISTRADOR, administratorService::findByEmail);
    }

    private String autenticar(User credenciales, Rol rol, Function<String, User> buscarPorEmail) {
        User registrado = buscarPorEmail.apply(credenciales.getEmail());
        if (registrado == null || !passwordValida(credenciales.getPassword(), registrado.getPassword())) {
            throw new AuthenticationException("Email o contraseña incorrectos");
        }
        return jwtTokenUtil.generateToken(registrado.getEmail(), rol, registrado.getId());
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
