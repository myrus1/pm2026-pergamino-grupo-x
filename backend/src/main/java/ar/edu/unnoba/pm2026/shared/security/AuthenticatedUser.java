package ar.edu.unnoba.pm2026.shared.security;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import java.util.Objects;

// Usuario resuelto desde el token valido: es el que usan los controladores y los
// services para saber quien pide la operacion y si le corresponde el rol o el recurso.
public record AuthenticatedUser(Long id, String email, Role role) {

    // El administrador siempre pasa; los demas solo sobre lo que les pertenece.
    public void requireOwnership(Long ownerId, String message) {
        if (role == Role.ADMINISTRATOR) {
            return;
        }
        if (!Objects.equals(id, ownerId)) {
            throw new AuthorizationException(message);
        }
    }
}
