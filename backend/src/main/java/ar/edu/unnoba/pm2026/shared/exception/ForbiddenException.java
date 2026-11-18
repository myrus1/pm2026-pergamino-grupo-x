package ar.edu.unnoba.pm2026.shared.exception;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        this("authorization.forbidden", message);
    }

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
