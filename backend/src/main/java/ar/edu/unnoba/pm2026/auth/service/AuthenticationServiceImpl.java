package ar.edu.unnoba.pm2026.auth.service;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.model.User;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final InstructorService instructorService;
    private final StudentService studentService;
    private final AdministratorService administratorService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationServiceImpl(InstructorService instructorService, StudentService studentService,
            AdministratorService administratorService, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.administratorService = administratorService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public String authenticate(Instructor instructor) {
        return authenticateUser(instructor, Role.INSTRUCTOR, instructorService::findByEmail);
    }

    @Override
    public String authenticate(Student student) {
        return authenticateUser(student, Role.STUDENT, studentService::findByEmail);
    }

    @Override
    public String authenticate(Administrator administrator) {
        return authenticateUser(administrator, Role.ADMINISTRATOR, administratorService::findByEmail);
    }

    private String authenticateUser(User credentials, Role role, Function<String, User> findUserByEmail) {
        User registeredUser = findUserByEmail.apply(credentials.getEmail());
        if (registeredUser == null || !isPasswordValid(credentials.getPassword(), registeredUser.getPassword())) {
            throw new AuthenticationException("Email o contraseña incorrectos");
        }
        return jwtTokenUtil.generateToken(registeredUser.getEmail(), role, registeredUser.getId());
    }

    // Password4j lanza IllegalArgumentException si la contraseña guardada no es un hash BCrypt
    private boolean isPasswordValid(String password, String storedHash) {
        try {
            return passwordEncoder.verify(password, storedHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
