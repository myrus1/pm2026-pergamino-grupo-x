package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student extends User {

    @Column(nullable = false, unique = true)
    private String dni;

    // Constrctor vacio que exige JPA
    public Student() {
        super();
    }

    public Student(String firstName, String lastName, String email, String password, String dni) {
        super(firstName, lastName, email, password);
        this.dni = dni;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }
}