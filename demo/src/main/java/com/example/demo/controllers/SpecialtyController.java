package com.example.demo.controllers;

import static com.example.demo.controllers.Validaciones.exigir;

import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.SpecialtyService;
import com.example.demo.dto.SpecialtyRequest;
import com.example.demo.dto.SpecialtyResponse;
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

// API REST de especialidades: /api/specialties
@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public List<SpecialtyResponse> getAll() {
        return specialtyService.getAll().stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public SpecialtyResponse getById(@PathVariable Long id) {
        return respuesta(specialtyService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyResponse create(@RequestBody SpecialtyRequest solicitud) {
        validar(solicitud);
        return respuesta(specialtyService.save(new Specialty(solicitud.getName())));
    }

    @PutMapping("/{id}")
    public SpecialtyResponse update(@PathVariable Long id, @RequestBody SpecialtyRequest solicitud) {
        validar(solicitud);
        Specialty especialidad = specialtyService.getById(id);
        especialidad.setName(solicitud.getName());
        return respuesta(specialtyService.save(especialidad));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        specialtyService.getById(id);
        specialtyService.delete(id);
    }

    private void validar(SpecialtyRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getName(), "name");
    }

    private SpecialtyResponse respuesta(Specialty especialidad) {
        return new SpecialtyResponse(especialidad.getId(), especialidad.getName());
    }
}
