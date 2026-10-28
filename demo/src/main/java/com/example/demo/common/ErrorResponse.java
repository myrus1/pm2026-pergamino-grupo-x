package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Cuerpo JSON uniforme para todos los errores de la API
@AllArgsConstructor
@Getter
public class ErrorResponse {

    private String mensaje;
}
