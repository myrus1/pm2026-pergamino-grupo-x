package ar.edu.unnoba.pm2026.user.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

public class InstructorEmailAlreadyExistsException extends BusinessRuleException {

    public InstructorEmailAlreadyExistsException(String email) {
        super(ErrorCode.INSTRUCTOR_EMAIL_ALREADY_EXISTS, email);
    }
}
