package com.example.demo.user.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.common.ErrorResponse;
import com.example.demo.user.dto.CreateInstructorRequestDTO;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorEmailAlreadyExistsException;
import com.example.demo.user.services.InstructorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/instructor")
public class InstructorResource {

    private final InstructorService instructorService;

    public InstructorResource(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateInstructorRequestDTO solicitud) {
        validar(solicitud);
        Instructor instructor = new Instructor(solicitud.firstName(), solicitud.lastName(), solicitud.email(),
                solicitud.password());
        try {
            instructorService.create(instructor);
        } catch (InstructorEmailAlreadyExistsException excepcion) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(excepcion.getMessage()));
        } catch (DataIntegrityViolationException excepcion) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Ya existe un usuario con el email " + solicitud.email()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private void validar(CreateInstructorRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.email(), "email");
        exigir(solicitud.password(), "password");
        exigir(solicitud.firstName(), "firstName");
        exigir(solicitud.lastName(), "lastName");
    }
}
