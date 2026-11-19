package ar.edu.unnoba.pm2026.testutil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // Igual que AuthorizationServiceImpl: devuelve el usuario si su rol esta entre los permitidos y si no
    // lanza 403. Asi un test con token de admin contra un endpoint de instructor falla como en la app.
    public static void as(AuthorizationService service, AuthenticatedUser authenticatedUser) {
        lenient().when(service.authorize(any(), any(Role[].class))).thenAnswer(invocation -> {
            if (!allowedRoles(invocation.getRawArguments()).contains(authenticatedUser.role())) {
                throw new AuthorizationException(ErrorCode.AUTHORIZATION_DENIED);
            }
            return authenticatedUser;
        });
    }

    private static List<Role> allowedRoles(Object[] rawArguments) {
        List<Role> roles = new ArrayList<>();
        for (int i = 1; i < rawArguments.length; i++) {
            if (rawArguments[i] instanceof Role[] array) {
                roles.addAll(Arrays.asList(array));
            } else if (rawArguments[i] instanceof Role role) {
                roles.add(role);
            }
        }
        return roles;
    }

    public static void asInstructor(AuthorizationService service, Long instructorId) {
        as(service, new AuthenticatedUser(instructorId, "iris@example.com", Role.INSTRUCTOR));
    }

    public static void asStudent(AuthorizationService service, Long studentId) {
        as(service, new AuthenticatedUser(studentId, "ana@example.com", Role.STUDENT));
    }

    // 401: sin header Authorization o token invalido/vencido
    public static void withoutToken(AuthorizationService service) {
        failWith(service, new AuthenticationException(ErrorCode.AUTHENTICATION_INVALID_TOKEN));
    }

    // 403: rol que no alcanza para la operacion
    public static void withoutPermission(AuthorizationService service) {
        failWith(service, new AuthorizationException(ErrorCode.AUTHORIZATION_DENIED));
    }

    public static void failWith(AuthorizationService service, RuntimeException exception) {
        lenient().when(service.authorize(any(), any(Role[].class))).thenThrow(exception);
    }
}
