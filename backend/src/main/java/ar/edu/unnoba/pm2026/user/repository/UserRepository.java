package ar.edu.unnoba.pm2026.user.repository;

import ar.edu.unnoba.pm2026.user.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Busqueda sobre la tabla base users: la usa el login unico para encontrar al usuario sin saber
// de antemano en que tabla hija esta. Con la herencia JOINED devuelve la subclase correcta.
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
