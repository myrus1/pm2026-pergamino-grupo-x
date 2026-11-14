package ar.edu.unnoba.pm2026.shared.exception;

import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.course.exception.CourseStateException;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Manejador global de errores de la API: responde siempre {"mensaje": ...}
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    // Sin credenciales validas: header Authorization ausente, token vencido o firma invalida
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(exception.getMessage()));
    }

    // Credenciales validas pero sin permiso: rol equivocado o recurso que no le pertenece
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AuthorizationException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InstructorEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInstructorEmail(
            InstructorEmailAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    // 23505: valor duplicado, 23503: registro en uso por otro (clave foranea)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String sqlState = exception.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        if ("23505".equals(sqlState) || "23503".equals(sqlState)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Los datos ya existen o estan en uso por otro registro"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Datos invalidos"));
    }

    @ExceptionHandler(ApprovalRequestException.class)
    public ResponseEntity<ErrorResponse> handleApprovalRequestRule(ApprovalRequestException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    // Reglas de negocio de inscripciones: curso no publicado, lleno, duplicado, etc.
    @ExceptionHandler(CourseEnrollmentException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentRule(CourseEnrollmentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    // El curso no esta en un estado que permita la operacion (p. ej. editar uno publicado)
    @ExceptionHandler(CourseStateException.class)
    public ResponseEntity<ErrorResponse> handleCourseState(CourseStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    // Las entidades validan las entradas con requireNonNull e IllegalArgumentException;
    // se mapean a 400 porque siempre responden a datos invalidos del cliente
    @ExceptionHandler({ IllegalArgumentException.class, NullPointerException.class })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exception.getMessage()));
    }

    // JSON mal formado o tipo de campo incompatible (p. ej. status invalido)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Cuerpo de la solicitud ilegible"));
    }
}
