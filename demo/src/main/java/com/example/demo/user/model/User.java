package com.example.demo.user.model;

import com.example.demo.common.Rol;
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

    // Rol del usuario dentro de la aplicacion. Se guarda para poder autenticar con un
    // unico login sin depender de en que tabla hija esta el usuario.
    // La columna queda nullable para que ddl-auto=update no falle con las filas que ya
    // existian antes de agregarla; @PostLoad completa esas filas al leerlas.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    protected Rol rol;

    public User() {
    }

    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    // Completa el rol de las filas creadas antes de que existiera la columna (o de una
    // entidad instanciada con el constructor vacio que exige JPA).
    @PrePersist
    @PostLoad
    private void completarRol() {
        if (rol != null) {
            return;
        }
        if (this instanceof Student) {
            rol = Rol.STUDENT;
        } else if (this instanceof Instructor) {
            rol = Rol.INSTRUCTOR;
        } else if (this instanceof Administrator) {
            rol = Rol.ADMINISTRATOR;
        }
    }

}