package com.example.demo.user.services;

public class InstructorEmailAlreadyExistsException extends RuntimeException {

    public InstructorEmailAlreadyExistsException(String email) {
        super("Ya existe un instructor con el email " + email);
    }
}
