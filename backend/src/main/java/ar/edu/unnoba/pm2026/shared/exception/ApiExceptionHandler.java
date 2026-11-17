package ar.edu.unnoba.pm2026.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception);
        return problem(status, exception.getMessage(), exception.getCode(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception,
            HttpServletRequest request) {
        String sqlState = exception.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        if ("23505".equals(sqlState) || "23503".equals(sqlState)) {
            return problem(HttpStatus.CONFLICT, "Los datos ya existen o estan en uso por otro registro",
                    "data.integrity.conflict", request);
        }
        return problem(HttpStatus.BAD_REQUEST, "Datos invalidos", "data.integrity.invalid", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Error inesperado al procesar {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado", "internal.error", request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest webRequest) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        HttpStatus httpStatus = HttpStatus.valueOf(status.value());
        String code = codeFor(exception, httpStatus);
        String detail = detailFor(exception, httpStatus);
        ProblemDetail problem;
        if (exception instanceof MethodArgumentNotValidException invalidRequest) {
            List<FieldViolation> errors = invalidRequest.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldViolation(error.getField(), validationMessage(error)))
                    .toList();
            detail = errors.isEmpty() ? detail : errors.get(0).message();
            problem = problem(httpStatus, detail, code, request, errors);
        } else {
            problem = problem(httpStatus, detail, code, request);
        }
        return new ResponseEntity<>(problem, headers, status);
    }

    private HttpStatus statusFor(ApiException exception) {
        if (exception instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof UnauthenticatedException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (exception instanceof ForbiddenException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof BusinessRuleException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String validationMessage(org.springframework.validation.FieldError error) {
        if ("NotBlank".equals(error.getCode()) || "NotNull".equals(error.getCode())) {
            return "El campo '" + error.getField() + "' es obligatorio";
        }
        return error.getDefaultMessage();
    }

    private String codeFor(Exception exception, HttpStatus status) {
        if (exception instanceof MethodArgumentNotValidException || exception instanceof HandlerMethodValidationException) {
            return "request.validation";
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "request.malformed";
        }
        if (exception instanceof NoResourceFoundException
                || exception instanceof org.springframework.web.servlet.NoHandlerFoundException) {
            return "http.not-found";
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return "http.method-not-allowed";
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return "http.unsupported-media-type";
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return "http.not-acceptable";
        }
        return "http." + status.value();
    }

    private String detailFor(Exception exception, HttpStatus status) {
        if (exception instanceof MethodArgumentNotValidException || exception instanceof HandlerMethodValidationException) {
            return "La solicitud contiene datos invalidos";
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "Cuerpo de la solicitud ilegible";
        }
        if (exception instanceof NoResourceFoundException
                || exception instanceof org.springframework.web.servlet.NoHandlerFoundException) {
            return "No se encontro la ruta solicitada";
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return "El metodo HTTP no esta permitido para esta ruta";
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return "El tipo de contenido no esta soportado";
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return "El formato de respuesta solicitado no esta soportado";
        }
        return status.getReasonPhrase();
    }

    private ProblemDetail problem(HttpStatus status, String detail, String code, HttpServletRequest request) {
        return problem(status, detail, code, request, null);
    }

    private ProblemDetail problem(HttpStatus status, String detail, String code, HttpServletRequest request,
            List<FieldViolation> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }

    private record FieldViolation(String field, String message) {
    }
}
