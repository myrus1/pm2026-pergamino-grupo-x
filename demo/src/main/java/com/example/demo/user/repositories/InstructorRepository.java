package com.example.demo.user.repositories;

import com.example.demo.user.model.Instructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    @Query("SELECT instructor FROM Instructor instructor WHERE instructor.email = :email")
    Instructor findByEmail(@Param("email") String email);
}
