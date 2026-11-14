package ar.edu.unnoba.pm2026.shared.validation;

// Validacion manual de campos obligatorios de los requests
// (el proyecto no usa jakarta.validation). Es transversal a todas las features.
public final class RequestValidator {

    private RequestValidator() {
    }

    public static void requireField(Object value, String field) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            throw new IllegalArgumentException("El campo '" + field + "' es obligatorio");
        }
    }
}
