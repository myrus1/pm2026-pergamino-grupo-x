package ar.edu.unnoba.pm2026.shared.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Cuerpo JSON uniforme para todos los errores de la API.
// El campo se sigue llamando "mensaje" en el JSON para no cambiar el contrato (ver #25).
@AllArgsConstructor
@Getter
public class ErrorResponse {

    @JsonProperty("mensaje")
    private String message;
}
