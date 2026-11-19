package ar.edu.unnoba.pm2026.shared.exception;

public class ForbiddenException extends ApiException {

    public ForbiddenException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
