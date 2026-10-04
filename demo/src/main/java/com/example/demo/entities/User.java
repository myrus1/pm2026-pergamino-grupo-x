package com.example.demo.entities;

import jakarta.persistence.*;

// Clase base abstracta para la jerarquia de usuarios.
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
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "first_name", nullable = false)
    protected String firstName;

    @Column(name = "last_name", nullable = false)
    protected String lastName;

    @Column(nullable = false, unique = true)
    protected String email;

    @Column(nullable = false)
    protected String password;

    public User() {
    }    //Constructo vacio que exige JPA

    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}