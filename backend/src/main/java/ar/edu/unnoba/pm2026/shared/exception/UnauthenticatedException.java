package ar.edu.unnoba.pm2026.shared.exception;

public class UnauthenticatedException extends ApiException {

    public UnauthenticatedException(String message) {
        this("authentication.unauthenticated", message);
    }

    public UnauthenticatedException(String code, String message) {
        super(code, message);
    }
}
