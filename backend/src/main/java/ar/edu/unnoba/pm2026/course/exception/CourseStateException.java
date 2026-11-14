package ar.edu.unnoba.pm2026.course.exception;

// Estado del curso que no permite la operacion (p. ej. editar uno que ya salio de
// borrador): responde 409 para que el cliente reintente por el flujo de aprobacion.
public class CourseStateException extends RuntimeException {

    public CourseStateException(String message) {
        super(message);
    }
}
