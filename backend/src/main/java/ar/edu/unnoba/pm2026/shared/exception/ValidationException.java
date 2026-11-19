package ar.edu.unnoba.pm2026.shared.exception;

public class ValidationException extends ApiException {

    public ValidationException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
