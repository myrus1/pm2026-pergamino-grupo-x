package ar.edu.unnoba.pm2026.course.model;

import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "courses")
@Getter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String title;

    @Setter
    private String description;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Setter
    @Column(nullable = false)
    private LocalDate startDate;

    @Setter
    @Column(nullable = false)
    private LocalDate endDate;

    @Setter
    @Column(nullable = false)
    private Integer maxCapacity;

    // Estado del curso guardado como String en la base de datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    // Especialidad del curso
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    // Relacion en la que muchos cursos pertenecen a un instructor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    // Lado inverso de la relacion: lo ignoramos para evitar ciclos al serializar a JSON
    @JsonIgnore
    @OneToMany(mappedBy = "course")
    private Set<Enrollment> enrollments = new HashSet<>();

    protected Course() {
    }

    public Course(String title, String description, BigDecimal basePrice,
                  LocalDate startDate, LocalDate endDate, Integer maxCapacity,
                  Specialty specialty, Instructor instructor) {
        validate(title, basePrice, startDate, endDate, maxCapacity);
        this.title = title;
        this.description = description;
        this.basePrice = basePrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxCapacity = maxCapacity;
        setSpecialty(specialty);
        setInstructor(instructor);
    }

    // Edicion de un curso (TP6: "solo los campos a modificar"). Un valor nulo deja el actual, y el resultado
    // se valida igual que en el constructor, asi una fecha de fin suelta no puede quedar antes del inicio.
    public void applyChanges(String title, String description, BigDecimal basePrice,
                             LocalDate startDate, LocalDate endDate, Integer maxCapacity) {
        String newTitle = title == null ? this.title : title;
        BigDecimal newBasePrice = basePrice == null ? this.basePrice : basePrice;
        LocalDate newStartDate = startDate == null ? this.startDate : startDate;
        LocalDate newEndDate = endDate == null ? this.endDate : endDate;
        Integer newMaxCapacity = maxCapacity == null ? this.maxCapacity : maxCapacity;
        validate(newTitle, newBasePrice, newStartDate, newEndDate, newMaxCapacity);
        this.title = newTitle;
        if (description != null) {
            this.description = description;
        }
        this.basePrice = newBasePrice;
        this.startDate = newStartDate;
        this.endDate = newEndDate;
        this.maxCapacity = newMaxCapacity;
    }

    private static void validate(String title, BigDecimal basePrice, LocalDate startDate, LocalDate endDate,
                                 Integer maxCapacity) {
        if (title == null) {
            throw new ValidationException("El titulo no puede ser nulo");
        }
        if (title.isBlank()) {
            throw new ValidationException("El titulo no puede estar vacio");
        }
        if (basePrice == null) {
            throw new ValidationException("El precio base no puede ser nulo");
        }
        if (basePrice.signum() < 0) {
            throw new ValidationException("El precio base no puede ser negativo");
        }
        if (startDate == null) {
            throw new ValidationException("La fecha de inicio no puede ser nula");
        }
        if (endDate == null) {
            throw new ValidationException("La fecha de fin no puede ser nula");
        }
        if (!endDate.isAfter(startDate)) {
            throw new ValidationException("La fecha de fin debe ser posterior a la de inicio");
        }
        if (maxCapacity == null) {
            throw new ValidationException("La capacidad no puede ser nula");
        }
        if (maxCapacity <= 0) {
            throw new ValidationException("La capacidad debe ser mayor a cero");
        }
    }

    public void setSpecialty(Specialty specialty) {
        if (this.specialty == specialty) {
            return;
        }
        if (specialty == null) {
            throw new ValidationException("La especialidad no puede ser nula");
        }
        Specialty previousSpecialty = this.specialty;
        this.specialty = specialty;
        if (previousSpecialty != null) {
            previousSpecialty.removeCourse(this);
        }
        specialty.addCourse(this);
    }

    public void setInstructor(Instructor instructor) {
        if (this.instructor == instructor) {
            return;
        }
        if (instructor == null) {
            throw new ValidationException("Instructor no puede ser nulo");
        }
        Instructor previousInstructor = this.instructor;
        this.instructor = instructor;
        if (previousInstructor != null) {
            previousInstructor.removeCourse(this);
        }
        instructor.addCourse(this);
    }

    public void setStatus(CourseStatus status) {
        if (status == null) {
            throw new ValidationException("Estado no puede ser nulo");
        }
        this.status = status;
    }

    public Set<Enrollment> getEnrollments() {
        return Collections.unmodifiableSet(enrollments);
    }

    public boolean hasAvailableCapacity() {
        return availableSeats() > 0;
    }

    public int enrolledStudents() {
        return enrollments.size();
    }

    // Lugares que quedan; nunca negativo aunque se haya bajado el cupo con inscriptos
    public int availableSeats() {
        return maxCapacity == null ? 0 : Math.max(maxCapacity - enrollments.size(), 0);
    }

    public void addEnrollment(Enrollment enrollment) {
        if (enrollment != null && enrollments.add(enrollment) && enrollment.getCourse() != this) {
            enrollment.setCourse(this);
        }
    }

    public void removeEnrollment(Enrollment enrollment) {
        enrollments.remove(enrollment);
    }
}
