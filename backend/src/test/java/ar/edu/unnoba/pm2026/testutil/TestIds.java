package ar.edu.unnoba.pm2026.testutil;

import java.lang.reflect.Field;

// Utilidad de test: asigna el id que JPA generaria al persistir (los ids no tienen setter)
public final class TestIds {

    private TestIds() {
    }

    public static void setId(Object target, Long id) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(target, id);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("No se pudo asignar el id en el test", e);
            }
        }
        throw new IllegalStateException("El objeto no tiene campo id");
    }
}
