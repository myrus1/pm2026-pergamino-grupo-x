package com.example.demo.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrators")
public class Administrator extends User {

    public Administrator() {
        super();
    }

    public Administrator(String firstName, String lastName, String email, String password) {
        super(firstName, lastName, email, password);
    }
}