package ar.edu.unnoba.pm2026.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// TP6: alta de administrador en POST /admin/accounts (CreateAdminRequestDTO: email, password, name, lastName).
// "name" tambien se acepta como "firstName", el nombre que usa /api/administrators.
@Getter
@Setter
public class CreateAdminRequest {

    @NotBlank
    @Email(message = "El campo 'email' debe tener un formato valido")
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    @JsonAlias("firstName")
    private String name;
    @NotBlank
    private String lastName;
}
