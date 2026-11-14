package ar.edu.unnoba.pm2026.user.repository;

import ar.edu.unnoba.pm2026.user.model.Instructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    @Query("SELECT instructor FROM Instructor instructor WHERE instructor.email = :email")
    Instructor findByEmail(@Param("email") String email);
}
