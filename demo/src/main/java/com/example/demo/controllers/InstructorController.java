package com.example.demo.controllers;

import static com.example.demo.controllers.Validaciones.exigir;

import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.SpecialtyService;
import com.example.demo.dto.InstructorRequest;
import com.example.demo.dto.InstructorResponse;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de instructores: /api/instructors (y asignacion de especialidades)
@RestController
@RequestMapping("/api/instructors")
public class InstructorController {

    private final InstructorService instructorService;
    private final SpecialtyService specialtyService;

    public InstructorController(InstructorService instructorService, SpecialtyService specialtyService) {
        this.instructorService = instructorService;
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public List<InstructorResponse> getAll() {
        return instructorService.getAll().stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public InstructorResponse getById(@PathVariable Long id) {
        return respuesta(instructorService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorResponse create(@RequestBody InstructorRequest solicitud) {
        validar(solicitud);
        Instructor instructor = new Instructor(solicitud.getFirstName(), solicitud.getLastName(),
                solicitud.getEmail(), solicitud.getPassword());
        return respuesta(instructorService.save(instructor));
    }

    @PutMapping("/{id}")
    public InstructorResponse update(@PathVariable Long id, @RequestBody InstructorRequest solicitud) {
        validar(solicitud);
        Instructor instructor = instructorService.getById(id);
        instructor.setFirstName(solicitud.getFirstName());
        instructor.setLastName(solicitud.getLastName());
        instructor.setEmail(solicitud.getEmail());
        instructor.setPassword(solicitud.getPassword());
        return respuesta(instructorService.save(instructor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        instructorService.getById(id);
        instructorService.delete(id);
    }

    @PutMapping("/{id}/specialties/{specialtyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignarEspecialidad(@PathVariable Long id, @PathVariable Long specialtyId) {
        Instructor instructor = instructorService.getById(id);
        Specialty especialidad = specialtyService.getById(specialtyId);
        instructor.addSpecialty(especialidad);
        instructorService.save(instructor);
    }

    @DeleteMapping("/{id}/specialties/{specialtyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarEspecialidad(@PathVariable Long id, @PathVariable Long specialtyId) {
        Instructor instructor = instructorService.getById(id);
        Specialty especialidad = specialtyService.getById(specialtyId);
        instructor.removeSpecialty(especialidad);
        instructorService.save(instructor);
    }

    private void validar(InstructorRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getFirstName(), "firstName");
        exigir(solicitud.getLastName(), "lastName");
        exigir(solicitud.getEmail(), "email");
        exigir(solicitud.getPassword(), "password");
    }

    private InstructorResponse respuesta(Instructor instructor) {
        return new InstructorResponse(instructor.getId(), instructor.getFirstName(), instructor.getLastName(),
                instructor.getEmail());
    }
}
