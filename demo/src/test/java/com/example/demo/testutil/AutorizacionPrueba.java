package com.example.demo.testutil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.example.demo.auth.model.AuthenticationException;
import com.example.demo.auth.model.AuthorizationException;
import com.example.demo.auth.services.AuthorizationService;
import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;

// Stubs de autorizacion para los tests de controladores (@WebMvcTest).
// porDefecto deja pasar a todos los tests como administrador; sinToken, sinPermiso
// y como* se usan para probar la matriz de permisos sin repetir el mismo when.
public final class AutorizacionPrueba {

    public static final String TOKEN = "Bearer token-de-prueba";
    public static final UsuarioAutenticado ADMIN = new UsuarioAutenticado(3L, "ada@example.com",
            Rol.ADMINISTRADOR);

    private AutorizacionPrueba() {
    }

    public static void porDefecto(AuthorizationService servicio) {
        como(servicio, ADMIN);
    }

    public static void como(AuthorizationService servicio, UsuarioAutenticado usuario) {
        lenient().when(servicio.autorizar(any(), any(Rol[].class))).thenReturn(usuario);
    }

    public static void comoInstructor(AuthorizationService servicio, Long instructorId) {
        como(servicio, new UsuarioAutenticado(instructorId, "iris@example.com", Rol.INSTRUCTOR));
    }

    public static void comoAlumno(AuthorizationService servicio, Long alumnoId) {
        como(servicio, new UsuarioAutenticado(alumnoId, "ana@example.com", Rol.ALUMNO));
    }

    // 401: sin header Authorization o token invalido/vencido
    public static void sinToken(AuthorizationService servicio) {
        queFalle(servicio, new AuthenticationException("Token invalido o vencido"));
    }

    // 403: rol que no alcanza para la operacion
    public static void sinPermiso(AuthorizationService servicio) {
        queFalle(servicio, new AuthorizationException("No tiene permisos para esta operacion"));
    }

    public static void queFalle(AuthorizationService servicio, RuntimeException excepcion) {
        lenient().when(servicio.autorizar(any(), any(Rol[].class))).thenThrow(excepcion);
    }
}
