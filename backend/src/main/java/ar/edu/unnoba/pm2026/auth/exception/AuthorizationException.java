package ar.edu.unnoba.pm2026.auth.exception;

import ar.edu.unnoba.pm2026.shared.exception.ForbiddenException;

// Credenciales validas pero sin permiso para la operacion (rol equivocado o recurso ajeno):
// responde 403. Es unchecked para que los services puedan validar el dueño del recurso.
public class AuthorizationException extends ForbiddenException {

    public AuthorizationException(String message) {
        super("authorization.denied", message);
    }
}
