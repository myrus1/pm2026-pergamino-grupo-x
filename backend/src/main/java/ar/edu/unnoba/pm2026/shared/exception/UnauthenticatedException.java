package ar.edu.unnoba.pm2026.shared.exception;

public class UnauthenticatedException extends ApiException {

    public UnauthenticatedException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
