package com.example.demo.auth.services;

import com.example.demo.user.model.Instructor;

public interface AuthenticationService {

    String authenticate(Instructor instructor) throws Exception;
}
