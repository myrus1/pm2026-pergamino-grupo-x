package ar.edu.unnoba.pm2026.approval.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

public class ApprovalRequestException extends BusinessRuleException {

    public ApprovalRequestException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
