package ar.edu.unnoba.pm2026.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Datos de entrada para crear o actualizar una especialidad
@Getter
@Setter
public class SpecialtyRequest {

    @NotBlank
    private String name;
}
