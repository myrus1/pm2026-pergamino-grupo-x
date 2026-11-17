package ar.edu.unnoba.pm2026.course.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;

// Estado del curso que no permite la operacion (p. ej. editar uno que ya salio de
// borrador): responde 409 para que el cliente reintente por el flujo de aprobacion.
public class CourseStateException extends BusinessRuleException {

    public CourseStateException(String message) {
        super(message);
    }
}
