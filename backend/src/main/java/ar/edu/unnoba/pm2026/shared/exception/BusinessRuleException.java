package ar.edu.unnoba.pm2026.shared.exception;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        this("business-rule.violation", message);
    }

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
