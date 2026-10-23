package com.example.demo.controllers;

// Validacion manual de campos obligatorios de los requests
// (el proyecto no usa jakarta.validation)
final class Validaciones {

    private Validaciones() {
    }

    static void exigir(Object valor, String campo) {
        if (valor == null || (valor instanceof String texto && texto.isBlank())) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
    }
}
