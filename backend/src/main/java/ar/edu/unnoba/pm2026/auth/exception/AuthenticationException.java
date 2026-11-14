package ar.edu.unnoba.pm2026.auth.exception;

// Falta de credenciales validas: responde 401.
// Es unchecked para que los servicios y controladores puedan lanzarla sin firmar el metodo.
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
