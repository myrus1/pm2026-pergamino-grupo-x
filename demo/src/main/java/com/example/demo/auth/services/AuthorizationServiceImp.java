package com.example.demo.auth.services;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.Rol;
import com.example.demo.user.model.User;
import com.example.demo.user.services.AdministratorService;
import com.example.demo.user.services.InstructorService;
import com.example.demo.user.services.StudentService;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationServiceImp implements AuthorizationService {

    private final JwtTokenUtil jwtTokenUtil;
    private final InstructorService instructorService;
    private final StudentService studentService;
    private final AdministratorService administratorService;

    public AuthorizationServiceImp(JwtTokenUtil jwtTokenUtil, InstructorService instructorService,
            StudentService studentService, AdministratorService administratorService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.administratorService = administratorService;
    }

    @Override
    public UsuarioAutenticado autorizar(String token, Rol... permitidos) {
        if (token == null || !jwtTokenUtil.verify(token)) {
            throw new AuthenticationException("Token invalido o vencido");
        }
        Rol rol = jwtTokenUtil.getRol(token);
        if (rol == null) {
            throw new AuthorizationException("El token no tiene rol asignado");
        }
        if (!Arrays.asList(permitidos).contains(rol)) {
            throw new AuthorizationException("No tiene permisos para esta operacion");
        }
        User usuario = buscarPorEmail(rol, jwtTokenUtil.getSubject(token));
        if (usuario == null) {
            throw new AuthorizationException("El usuario del token ya no existe");
        }
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), rol);
    }

    private User buscarPorEmail(Rol rol, String email) {
        return switch (rol) {
            case INSTRUCTOR -> instructorService.findByEmail(email);
            case ALUMNO -> studentService.findByEmail(email);
            case ADMINISTRADOR -> administratorService.findByEmail(email);
        };
    }
}
