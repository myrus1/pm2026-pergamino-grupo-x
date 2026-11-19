package ar.edu.unnoba.pm2026.shared.exception;

// Excepcion de la API: lleva un codigo (que es la clave del mensaje en messages*.properties)
// y los argumentos para interpolar ese mensaje. El texto no se guarda: ApiExceptionHandler
// lo resuelve con MessageSource segun el Accept-Language de la request.
public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Object[] arguments;

    protected ApiException(ErrorCode code, Object... arguments) {
        super(code.key());
        this.code = code;
        this.arguments = arguments;
    }

    public String getCode() {
        return code.key();
    }

    public Object[] getArguments() {
        return arguments;
    }
}
