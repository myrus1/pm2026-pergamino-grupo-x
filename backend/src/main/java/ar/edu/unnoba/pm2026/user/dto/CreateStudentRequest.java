package ar.edu.unnoba.pm2026.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// TP6: registro de alumno en POST /students (CreateStudentRequestDTO: name, lastName, dni, email, password).
// "name" tambien se acepta como "firstName", el nombre que usa /api/students.
@Getter
@Setter
public class CreateStudentRequest {

    @NotBlank
    @JsonAlias("firstName")
    private String name;
    @NotBlank
    private String lastName;
    @NotBlank
    private String dni;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
}
