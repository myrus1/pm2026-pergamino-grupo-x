package ar.edu.unnoba.pm2026.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

// Traduce las excepciones a ProblemDetail. Los textos salen de messages.properties (es) y
// messages_en.properties (en), resueltos con el Accept-Language de la request; el espanol es
// el idioma por defecto. Las excepciones solo llevan un ErrorCode y sus argumentos.
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Claves de los mensajes de Bean Validation. No son ErrorCode porque dependen del
    // constraint que fallo, no del tipo de excepcion. El campo viaja como argumento {0}.
    static final Map<String, String> VALIDATION_KEYS = Map.of(
            "NotBlank", "validation.required",
            "NotNull", "validation.required",
            "NotEmpty", "validation.required",
            "Email", "validation.email",
            "Positive", "validation.positive",
            "DecimalMin", "validation.decimal-min",
            "Pattern", "validation.pattern");

    static final String VALIDATION_INVALID = "validation.invalid";

    private final MessageSource messages;
    private final Locale defaultLocale;

    public ApiExceptionHandler(MessageSource messages,
            @Value("${spring.web.locale:es}") String defaultLocaleTag) {
        this.messages = messages;
        this.defaultLocale = Locale.forLanguageTag(defaultLocaleTag);
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception);
        return problem(status, text(request, exception.getCode(), exception.getArguments()),
                exception.getCode(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception,
            HttpServletRequest request) {
        String sqlState = exception.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        if ("23505".equals(sqlState) || "23503".equals(sqlState)) {
            return problem(HttpStatus.CONFLICT,
                    text(request, ErrorCode.DATA_INTEGRITY_CONFLICT.key()),
                    ErrorCode.DATA_INTEGRITY_CONFLICT.key(), request);
        }
        return problem(HttpStatus.BAD_REQUEST,
                text(request, ErrorCode.DATA_INTEGRITY_INVALID.key()),
                ErrorCode.DATA_INTEGRITY_INVALID.key(), request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected error processing request {} [code={}]",
                request.getRequestURI(), ErrorCode.INTERNAL_ERROR.key(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                text(request, ErrorCode.INTERNAL_ERROR.key()),
                ErrorCode.INTERNAL_ERROR.key(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest webRequest) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        HttpStatus httpStatus = HttpStatus.valueOf(status.value());
        String code = codeFor(exception, httpStatus);
        String detail = detailFor(request, exception, httpStatus);
        ProblemDetail problem;
        if (exception instanceof MethodArgumentNotValidException invalidRequest) {
            List<FieldViolation> errors = invalidRequest.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldViolation(error.getField(), validationMessage(request, error)))
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

    private String validationMessage(HttpServletRequest request, org.springframework.validation.FieldError error) {
        String key = VALIDATION_KEYS.getOrDefault(error.getCode(), VALIDATION_INVALID);
        return text(request, key, error.getField());
    }

    private String codeFor(Exception exception, HttpStatus status) {
        if (exception instanceof MethodArgumentNotValidException || exception instanceof HandlerMethodValidationException) {
            return ErrorCode.REQUEST_VALIDATION.key();
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return ErrorCode.REQUEST_MALFORMED.key();
        }
        if (exception instanceof NoResourceFoundException
                || exception instanceof org.springframework.web.servlet.NoHandlerFoundException) {
            return ErrorCode.HTTP_NOT_FOUND.key();
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return ErrorCode.HTTP_METHOD_NOT_ALLOWED.key();
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return ErrorCode.HTTP_UNSUPPORTED_MEDIA_TYPE.key();
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return ErrorCode.HTTP_NOT_ACCEPTABLE.key();
        }
        // Parametros de consulta (p. ej. fromDate/toDate de los reportes del TP6)
        if (exception instanceof MissingServletRequestParameterException) {
            return ErrorCode.REQUEST_MISSING_PARAMETER.key();
        }
        if (exception instanceof TypeMismatchException) {
            return ErrorCode.REQUEST_INVALID_PARAMETER.key();
        }
        return "http." + status.value();
    }

    private String detailFor(HttpServletRequest request, Exception exception, HttpStatus status) {
        if (exception instanceof MethodArgumentNotValidException || exception instanceof HandlerMethodValidationException) {
            return text(request, ErrorCode.REQUEST_VALIDATION.key());
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return text(request, ErrorCode.REQUEST_MALFORMED.key());
        }
        if (exception instanceof NoResourceFoundException
                || exception instanceof org.springframework.web.servlet.NoHandlerFoundException) {
            return text(request, ErrorCode.HTTP_NOT_FOUND.key());
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return text(request, ErrorCode.HTTP_METHOD_NOT_ALLOWED.key());
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return text(request, ErrorCode.HTTP_UNSUPPORTED_MEDIA_TYPE.key());
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return text(request, ErrorCode.HTTP_NOT_ACCEPTABLE.key());
        }
        if (exception instanceof MissingServletRequestParameterException missing) {
            return text(request, ErrorCode.REQUEST_MISSING_PARAMETER.key(), missing.getParameterName());
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return text(request, ErrorCode.REQUEST_INVALID_PARAMETER.key(), mismatch.getName());
        }
        return textOrDefault(request, "http." + status.value(), status.getReasonPhrase());
    }

    // Resuelve la clave en el idioma de la request. Si la clave no existe devuelve la clave
    // (asi una excepcion vieja que todavia lleve el texto como codigo sigue siendo legible).
    private String text(HttpServletRequest request, String key, Object... arguments) {
        return messages.getMessage(key, arguments, key, locale(request));
    }

    private String textOrDefault(HttpServletRequest request, String key, String fallback, Object... arguments) {
        String resolved = messages.getMessage(key, arguments, null, locale(request));
        return resolved != null ? resolved : fallback;
    }

    // El Accept-Language de la request fija el idioma. El filtro de Actuator resuelve sus
    // excepciones fuera del DispatcherServlet, donde no hay LocaleContext: ahi se usa el
    // header (si vino) o el idioma por defecto configurado (es).
    private Locale locale(HttpServletRequest request) {
        LocaleContext context = LocaleContextHolder.getLocaleContext();
        if (context != null) {
            return context.getLocale();
        }
        String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            return request.getLocale();
        }
        return defaultLocale;
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
