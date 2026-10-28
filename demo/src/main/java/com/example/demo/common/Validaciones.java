package com.example.demo.common;

// Validacion manual de campos obligatorios de los requests
// (el proyecto no usa jakarta.validation). Es transversal a todas las features.
public final class Validaciones {

    private Validaciones() {
    }

    public static void exigir(Object valor, String campo) {
        if (valor == null || (valor instanceof String texto && texto.isBlank())) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
    }
}
