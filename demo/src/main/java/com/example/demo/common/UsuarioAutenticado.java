package com.example.demo.common;

import com.example.demo.auth.model.AuthorizationException;
import java.util.Objects;

// Usuario resuelto desde el token valido: es el que usan los controladores y los
// services para saber quien pide la operacion y si le corresponde el rol o el recurso.
public record UsuarioAutenticado(Long id, String email, Rol rol) {

    // El administrador siempre pasa; los demas solo sobre lo que les pertenece.
    public void exigirPropio(Long idPropietario, String mensaje) {
        if (rol == Rol.ADMINISTRADOR) {
            return;
        }
        if (!Objects.equals(id, idPropietario)) {
            throw new AuthorizationException(mensaje);
        }
    }
}
