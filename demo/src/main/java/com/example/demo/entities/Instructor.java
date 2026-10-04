package com.example.demo.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "instructors")
public class Instructor extends User {

    // Almacena la coleccion de enums en una tabla auxiliar normalizada 'instructor_specialties'
    @ElementCollection(targetClass = Specialty.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "instructor_specialties", joinColumns = @JoinColumn(name = "instructor_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "specialty")
    private Set<Specialty> specialties = new HashSet<>();

    // Constructor vacio  JPA
    public Instructor() {
        super();
    }

    public Instructor(String firstName, String lastName, String email, String password) {
        super(firstName, lastName, email, password);
    }

    public Set<Specialty> getSpecialties() {
        return specialties;
    }

    public void setSpecialties(Set<Specialty> specialties) {
        this.specialties = specialties;
    }
}

