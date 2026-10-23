package com.example.demo.user.controllers;

import static com.example.demo.controllers.Validaciones.exigir;

import com.example.demo.user.dto.AdministratorRequest;
import com.example.demo.user.dto.AdministratorResponse;
import com.example.demo.user.model.Administrator;
import com.example.demo.user.services.AdministratorService;
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

// API REST de administradores: /api/administrators
@RestController
@RequestMapping("/api/administrators")
public class AdministratorController {

    private final AdministratorService administratorService;

    public AdministratorController(AdministratorService administratorService) {
        this.administratorService = administratorService;
    }

    @GetMapping
    public List<AdministratorResponse> getAll() {
        return administratorService.getAll().stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public AdministratorResponse getById(@PathVariable Long id) {
        return respuesta(administratorService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdministratorResponse create(@RequestBody AdministratorRequest solicitud) {
        validar(solicitud);
        Administrator administrador = new Administrator(solicitud.getFirstName(), solicitud.getLastName(),
                solicitud.getEmail(), solicitud.getPassword());
        return respuesta(administratorService.save(administrador));
    }

    @PutMapping("/{id}")
    public AdministratorResponse update(@PathVariable Long id, @RequestBody AdministratorRequest solicitud) {
        validar(solicitud);
        Administrator administrador = administratorService.getById(id);
        administrador.setFirstName(solicitud.getFirstName());
        administrador.setLastName(solicitud.getLastName());
        administrador.setEmail(solicitud.getEmail());
        administrador.setPassword(solicitud.getPassword());
        return respuesta(administratorService.save(administrador));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        administratorService.getById(id);
        administratorService.delete(id);
    }

    private void validar(AdministratorRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getFirstName(), "firstName");
        exigir(solicitud.getLastName(), "lastName");
        exigir(solicitud.getEmail(), "email");
        exigir(solicitud.getPassword(), "password");
    }

    private AdministratorResponse respuesta(Administrator administrador) {
        return new AdministratorResponse(administrador.getId(), administrador.getFirstName(),
                administrador.getLastName(), administrador.getEmail());
    }
}
