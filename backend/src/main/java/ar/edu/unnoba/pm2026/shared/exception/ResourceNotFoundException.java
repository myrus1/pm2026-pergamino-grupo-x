package ar.edu.unnoba.pm2026.shared.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
