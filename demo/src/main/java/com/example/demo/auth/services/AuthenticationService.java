package com.example.demo.auth.services;

import com.example.demo.auth.dto.AuthenticationResult;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;

public interface AuthenticationService {

    // Login unico: busca el email en users y usa el rol guardado en el usuario.
    AuthenticationResult authenticate(String email, String password);

    // Logins historicos por rol. Siguen funcionando y devuelven {"token": "Bearer ..."}.
    String authenticate(Instructor instructor);

    String authenticate(Student student);

    String authenticate(Administrator administrator);
}
