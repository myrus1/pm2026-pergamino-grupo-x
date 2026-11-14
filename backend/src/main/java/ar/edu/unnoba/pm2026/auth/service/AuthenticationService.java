package ar.edu.unnoba.pm2026.auth.service;

import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;

public interface AuthenticationService {

    String authenticate(Instructor instructor);

    String authenticate(Student student);

    String authenticate(Administrator administrator);
}
