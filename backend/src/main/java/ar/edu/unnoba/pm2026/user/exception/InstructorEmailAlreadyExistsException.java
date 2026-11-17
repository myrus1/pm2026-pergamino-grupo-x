package ar.edu.unnoba.pm2026.user.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;

public class InstructorEmailAlreadyExistsException extends BusinessRuleException {

    public InstructorEmailAlreadyExistsException(String email) {
        super("Ya existe un instructor con el email " + email);
    }
}
