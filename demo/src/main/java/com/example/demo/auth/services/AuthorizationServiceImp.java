package com.example.demo.auth.services;

import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationServiceImp implements AuthorizationService {

    private final JwtTokenUtil jwtTokenUtil;
    private final InstructorService instructorService;

    public AuthorizationServiceImp(JwtTokenUtil jwtTokenUtil, InstructorService instructorService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.instructorService = instructorService;
    }

    @Override
    public Instructor authorize(String token) throws Exception {
        if (!jwtTokenUtil.verify(token)) {
            throw new AuthorizationException("Token invalido o vencido");
        }
        Instructor instructor = instructorService.findByEmail(jwtTokenUtil.getSubject(token));
        if (instructor == null) {
            throw new AuthorizationException("El token no corresponde a un instructor");
        }
        return instructor;
    }
}
