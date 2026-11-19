package ar.edu.unnoba.pm2026.auth.exception;

import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.exception.UnauthenticatedException;

// Falta de credenciales validas: responde 401.
// Es unchecked para que los servicios y controladores puedan lanzarla sin firmar el metodo.
public class AuthenticationException extends UnauthenticatedException {

    public AuthenticationException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
