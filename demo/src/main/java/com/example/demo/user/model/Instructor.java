package com.example.demo.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "instructors")
public class Instructor extends User {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "instructor_specialties",
            joinColumns = @JoinColumn(name = "instructor_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id"))
    private Set<Specialty> specialties = new HashSet<>();

    @OneToMany(mappedBy = "instructor")
    private Set<Course> courses = new HashSet<>();

    public Instructor() {
        super();
    }

    public Instructor(String firstName, String lastName, String email, String password) {
        super(firstName, lastName, email, password);
    }

    public Course createCourse(String title, String description, BigDecimal basePrice,
                               LocalDate startDate, LocalDate endDate, Integer maxCapacity,
                               Specialty specialty) {
        Objects.requireNonNull(specialty, "especialidad no puede ser nula");
        if (!specialties.contains(specialty)) {
            throw new IllegalArgumentException("instructor debe tener la especilidad del curso");
        }

        return new Course(title, description, basePrice, startDate, endDate, maxCapacity, specialty, this);
    }

    public Set<Specialty> getSpecialties() {
        return Collections.unmodifiableSet(specialties);
    }

    public void setSpecialties(Set<Specialty> specialties) {
        Set<Specialty> newSpecialties = specialties == null ? new HashSet<>() : new HashSet<>(specialties);
        for (Specialty specialty : new HashSet<>(this.specialties)) {
            removeSpecialty(specialty);
        }
        for (Specialty specialty : newSpecialties) {
            addSpecialty(specialty);
        }
    }

    public void addSpecialty(Specialty specialty) {
        if (specialty != null && this.specialties.add(specialty)) {
            specialty.addInstructor(this);
        }
    }

    public void removeSpecialty(Specialty specialty) {
        if (this.specialties.remove(specialty) && specialty != null) {
            specialty.removeInstructor(this);
        }
    }

    public Set<Course> getCourses() {
        return Collections.unmodifiableSet(courses);
    }

    void addCourse(Course course) {
        if (course != null && this.courses.add(course) && course.getInstructor() != this) {
            course.setInstructor(this);
        }
    }

    void removeCourse(Course course) {
        this.courses.remove(course);
    }
}
