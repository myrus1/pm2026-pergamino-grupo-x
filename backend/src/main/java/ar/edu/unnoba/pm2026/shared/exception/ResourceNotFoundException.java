package ar.edu.unnoba.pm2026.shared.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        this("resource.not-found", message);
    }

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
