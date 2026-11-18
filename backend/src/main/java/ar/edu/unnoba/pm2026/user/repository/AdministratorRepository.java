package ar.edu.unnoba.pm2026.user.repository;

import ar.edu.unnoba.pm2026.user.model.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    @Query("SELECT administrator FROM Administrator administrator WHERE administrator.email = :email")
    Administrator findByEmail(@Param("email") String email);
}
