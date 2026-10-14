package com.example.demo.entities;

import jakarta.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "specialties")
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @Column(nullable = false, unique = true)
    @Getter
    @Setter
    private String name;

    @ManyToMany(mappedBy = "specialties")
    private Set<Instructor> instructors = new HashSet<>();

    @OneToMany(mappedBy = "specialty")
    private Set<Course> courses = new HashSet<>();

    public Specialty() {
    }

    public Specialty(String name) {
        this.name = name;
    }

    public Set<Instructor> getInstructors() {
        return Collections.unmodifiableSet(instructors);
    }

    public void setInstructors(Set<Instructor> instructors) {
        Set<Instructor> newInstructors = instructors == null ? new HashSet<>() : new HashSet<>(instructors);
        for (Instructor instructor : new HashSet<>(this.instructors)) {
            removeInstructor(instructor);
        }
        for (Instructor instructor : newInstructors) {
            addInstructor(instructor);
        }
    }

    public void addInstructor(Instructor instructor) {
        if (instructor != null && this.instructors.add(instructor)) {
            instructor.addSpecialty(this);
        }
    }

    public void removeInstructor(Instructor instructor) {
        if (this.instructors.remove(instructor) && instructor != null) {
            instructor.removeSpecialty(this);
        }
    }

    public Set<Course> getCourses() {
        return Collections.unmodifiableSet(courses);
    }

    void addCourse(Course course) {
        if (course != null && this.courses.add(course) && course.getSpecialty() != this) {
            course.setSpecialty(this);
        }
    }

    void removeCourse(Course course) {
        this.courses.remove(course);
    }
}