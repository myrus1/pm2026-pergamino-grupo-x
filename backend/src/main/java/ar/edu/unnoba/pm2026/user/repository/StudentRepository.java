package ar.edu.unnoba.pm2026.user.repository;

import ar.edu.unnoba.pm2026.user.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT student FROM Student student WHERE student.email = :email")
    Student findByEmail(@Param("email") String email);
}
