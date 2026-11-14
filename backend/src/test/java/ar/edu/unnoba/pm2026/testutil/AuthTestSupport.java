package ar.edu.unnoba.pm2026.testutil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;

// Stubs de autorizacion para los tests de controladores (@WebMvcTest).
// porDefecto deja pasar a todos los tests como administrador; sinToken, sinPermiso
// y como* se usan para probar la matriz de permisos sin repetir el mismo when.
public final class AuthTestSupport {

    public static final String TOKEN = "Bearer token-de-prueba";
    public static final AuthenticatedUser ADMIN = new AuthenticatedUser(3L, "ada@example.com",
            Role.ADMINISTRATOR);

    private AuthTestSupport() {
    }

    public static void asDefaultAdministrator(AuthorizationService service) {
        as(service, ADMIN);
    }

    public static void as(AuthorizationService service, AuthenticatedUser authenticatedUser) {
        lenient().when(service.authorize(any(), any(Role[].class))).thenReturn(authenticatedUser);
    }

    public static void asInstructor(AuthorizationService service, Long instructorId) {
        as(service, new AuthenticatedUser(instructorId, "iris@example.com", Role.INSTRUCTOR));
    }

    public static void asStudent(AuthorizationService service, Long studentId) {
        as(service, new AuthenticatedUser(studentId, "ana@example.com", Role.STUDENT));
    }

    // 401: sin header Authorization o token invalido/vencido
    public static void withoutToken(AuthorizationService service) {
        failWith(service, new AuthenticationException("Token invalido o vencido"));
    }

    // 403: rol que no alcanza para la operacion
    public static void withoutPermission(AuthorizationService service) {
        failWith(service, new AuthorizationException("No tiene permisos para esta operacion"));
    }

    public static void failWith(AuthorizationService service, RuntimeException exception) {
        lenient().when(service.authorize(any(), any(Role[].class))).thenThrow(exception);
    }
}
