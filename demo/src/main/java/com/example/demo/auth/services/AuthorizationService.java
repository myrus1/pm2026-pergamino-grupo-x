package com.example.demo.auth.services;

import com.example.demo.auth.model.UsuarioAutenticado;
import com.example.demo.common.Rol;

public interface AuthorizationService {

    // Valida el token y que su rol este entre los permitidos para la operacion.
    // Lanza AuthenticationException (401) si el token falta o es invalido y
    // AuthorizationException (403) si el rol no alcanza o el usuario ya no existe.
    UsuarioAutenticado autorizar(String token, Rol... permitidos);
}
