package ar.edu.unnoba.pm2026.user.exception;

public class InstructorEmailAlreadyExistsException extends RuntimeException {

    public InstructorEmailAlreadyExistsException(String email) {
        super("Ya existe un instructor con el email " + email);
    }
}
