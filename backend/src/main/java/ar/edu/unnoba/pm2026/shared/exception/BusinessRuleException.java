package ar.edu.unnoba.pm2026.shared.exception;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
