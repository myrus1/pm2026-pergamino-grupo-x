package ar.edu.unnoba.pm2026.shared.exception;

public class ValidationException extends ApiException {

    public ValidationException(String message) {
        this("request.validation", message);
    }

    public ValidationException(String code, String message) {
        super(code, message);
    }
}
