package com.example.demo.auth.model;

// Falta de credenciales validas: responde 401.
// Es unchecked para que los servicios y controladores puedan lanzarla sin firmar el metodo.
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
