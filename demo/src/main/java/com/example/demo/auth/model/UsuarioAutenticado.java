package com.example.demo.auth.model;

import com.example.demo.common.Rol;

// Usuario resuelto desde el token valido: es el que usan los controladores
// para saber quien pide la operacion y si le corresponde el rol.
public record UsuarioAutenticado(Long id, String email, Rol rol) {
}
