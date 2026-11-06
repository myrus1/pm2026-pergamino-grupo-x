package com.example.demo.auth.services;

import com.example.demo.user.model.Instructor;

public interface AuthorizationService {

    Instructor authorize(String token) throws Exception;
}
