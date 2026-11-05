package com.example.demo.common;

import com.example.demo.approval.model.ApprovalRequestException;
import com.example.demo.enrollment.model.CourseEnrollmentException;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
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
    public ResponseEntity<ErrorResponse> recursoNoEncontrado(EntityNotFoundException excepcion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(excepcion.getMessage()));
    }

    @ExceptionHandler(InstructorEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> emailDeInstructorDuplicado(
            InstructorEmailAlreadyExistsException excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(excepcion.getMessage()));
    }

    // 23505: valor duplicado, 23503: registro en uso por otro (clave foranea)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> datosEnConflicto(DataIntegrityViolationException excepcion) {
        String estado = excepcion.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        if ("23505".equals(estado) || "23503".equals(estado)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Los datos ya existen o estan en uso por otro registro"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Datos invalidos"));
    }

    @ExceptionHandler(ApprovalRequestException.class)
    public ResponseEntity<ErrorResponse> aprobacionRechazada(ApprovalRequestException excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(excepcion.getMessage()));
    }

    // Reglas de negocio de inscripciones: curso no publicado, lleno, duplicado, etc.
    @ExceptionHandler(CourseEnrollmentException.class)
    public ResponseEntity<ErrorResponse> inscripcionRechazada(CourseEnrollmentException excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(excepcion.getMessage()));
    }

    // Las entidades validan las entradas con requireNonNull e IllegalArgumentException;
    // se mapean a 400 porque siempre responden a datos invalidos del cliente
    @ExceptionHandler({ IllegalArgumentException.class, NullPointerException.class })
    public ResponseEntity<ErrorResponse> solicitudInvalida(RuntimeException excepcion) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(excepcion.getMessage()));
    }

    // JSON mal formado o tipo de campo incompatible (p. ej. status invalido)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException excepcion) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Cuerpo de la solicitud ilegible"));
    }
}
