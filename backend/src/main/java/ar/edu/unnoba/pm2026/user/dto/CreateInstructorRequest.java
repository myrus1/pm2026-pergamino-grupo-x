package ar.edu.unnoba.pm2026.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// TP6: POST /instructor (CreateInstructorRequestDTO: email y password). Nombre y apellido son opcionales hasta
// que se resuelva el #13: si no vienen se guardan vacios, porque la tabla users los pide NOT NULL.
// "name" tambien se acepta como "firstName", como lo mandaba el TP4.
public record CreateInstructorRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @JsonAlias("firstName") String name,
        String lastName) {

    public String nameOrEmpty() {
        return name == null ? "" : name.trim();
    }

    public String lastNameOrEmpty() {
        return lastName == null ? "" : lastName.trim();
    }
}
