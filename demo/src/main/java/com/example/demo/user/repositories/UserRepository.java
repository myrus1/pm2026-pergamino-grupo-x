package com.example.demo.user.repositories;

import com.example.demo.user.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Busqueda comun sobre la tabla base users: la usa el login unico para resolver
// el rol sin saber de antemano en que tabla hija esta el usuario.
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
