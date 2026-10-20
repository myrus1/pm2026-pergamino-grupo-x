package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/**
 * Aclaracion sobre uso de Herencia JOINED:
 * Crea una tabla base ('users') con atributos comunes
 * y tablas separadas para cada rol ('students', 'instructors', 'administrators')
 * vinculadas por ID (FK). Evita campos nulos (NULL) innecesarios y mantiene
 * la base de datos normalizada según los requerimientos del DER.
 */

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(nullable = false)
    protected String firstName;

    @Column(nullable = false)
    protected String lastName;

    @Column(nullable = false, unique = true)
    protected String email;

    @Column(nullable = false)
    protected String password;

    public User() {
    }

    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

}