package com.example.demo.testutil;

import java.lang.reflect.Field;

// Utilidad de test: asigna el id que JPA generaria al persistir (los ids no tienen setter)
public final class TestIds {

    private TestIds() {
    }

    public static void fijarId(Object objeto, Long id) {
        Class<?> tipo = objeto.getClass();
        while (tipo != null) {
            try {
                Field campo = tipo.getDeclaredField("id");
                campo.setAccessible(true);
                campo.set(objeto, id);
                return;
            } catch (NoSuchFieldException e) {
                tipo = tipo.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("No se pudo asignar el id en el test", e);
            }
        }
        throw new IllegalStateException("El objeto no tiene campo id");
    }
}
