package ar.edu.unnoba.pm2026.approval.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;

public class ApprovalRequestException extends BusinessRuleException {

    public ApprovalRequestException(String message) {
        this("approval.rule.violation", message);
    }

    public ApprovalRequestException(String code, String message) {
        super(code, message);
    }
}
