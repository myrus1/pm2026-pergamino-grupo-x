package ar.edu.unnoba.pm2026.auth.service;

import ar.edu.unnoba.pm2026.auth.dto.AuthenticationResult;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;

public interface AuthenticationService {

    // Login unico: busca el email en users y usa el rol del usuario encontrado.
    AuthenticationResult authenticate(String email, String password);

    // Logins por rol: cada uno acepta solo su rol y devuelve "Bearer <jwt>".
    String authenticate(Instructor instructor);

    String authenticate(Student student);

    String authenticate(Administrator administrator);
}
