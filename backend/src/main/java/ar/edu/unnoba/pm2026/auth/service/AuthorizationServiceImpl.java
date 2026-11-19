package ar.edu.unnoba.pm2026.auth.service;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.user.model.User;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private final JwtTokenUtil jwtTokenUtil;
    private final InstructorService instructorService;
    private final StudentService studentService;
    private final AdministratorService administratorService;

    public AuthorizationServiceImpl(JwtTokenUtil jwtTokenUtil, InstructorService instructorService,
            StudentService studentService, AdministratorService administratorService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.administratorService = administratorService;
    }

    @Override
    public AuthenticatedUser authorize(String token, Role... allowedRoles) {
        if (token == null || !jwtTokenUtil.verify(token)) {
            throw new AuthenticationException(ErrorCode.AUTHENTICATION_INVALID_TOKEN);
        }
        Role role;
        try {
            role = jwtTokenUtil.getRole(token);
        } catch (IllegalArgumentException exception) {
            // Valor de rol que no existe: pedir login de nuevo
            throw new AuthenticationException(ErrorCode.AUTHENTICATION_INVALID_TOKEN);
        }
        if (role == null) {
            throw new AuthorizationException(ErrorCode.AUTHORIZATION_ROLE_REQUIRED);
        }
        if (!Arrays.asList(allowedRoles).contains(role)) {
            throw new AuthorizationException(ErrorCode.AUTHORIZATION_DENIED);
        }
        User authenticatedUser = findUserByEmail(role, jwtTokenUtil.getSubject(token));
        if (authenticatedUser == null) {
            throw new AuthorizationException(ErrorCode.AUTHORIZATION_USER_NOT_FOUND);
        }
        return new AuthenticatedUser(authenticatedUser.getId(), authenticatedUser.getEmail(), role);
    }

    private User findUserByEmail(Role role, String email) {
        return switch (role) {
            case INSTRUCTOR -> instructorService.findByEmail(email);
            case STUDENT -> studentService.findByEmail(email);
            case ADMINISTRATOR -> administratorService.findByEmail(email);
        };
    }
}
