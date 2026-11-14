package ar.edu.unnoba.pm2026.course.repository;

import ar.edu.unnoba.pm2026.course.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
}
