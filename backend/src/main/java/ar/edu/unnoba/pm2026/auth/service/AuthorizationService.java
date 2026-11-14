package ar.edu.unnoba.pm2026.auth.service;

import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;

public interface AuthorizationService {

    // Valida el token y que su rol este entre los permitidos para la operacion.
    // Lanza AuthenticationException (401) si el token falta o es invalido y
    // AuthorizationException (403) si el rol no alcanza o el usuario ya no existe.
    AuthenticatedUser authorize(String token, Role... allowedRoles);
}
