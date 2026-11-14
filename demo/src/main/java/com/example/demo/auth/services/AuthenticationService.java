package com.example.demo.auth.services;

import com.example.demo.user.model.Administrator;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.model.Student;

public interface AuthenticationService {

    String authenticate(Instructor instructor);

    String authenticate(Student student);

    String authenticate(Administrator administrator);
}
