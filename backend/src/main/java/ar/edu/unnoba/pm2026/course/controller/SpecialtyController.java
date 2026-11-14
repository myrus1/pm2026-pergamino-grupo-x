package ar.edu.unnoba.pm2026.course.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.SpecialtyService;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyRequest;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de especialidades: /api/specialties (consultar es publico, escribir es de administrador)
@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;
    private final AuthorizationService authorizationService;

    public SpecialtyController(SpecialtyService specialtyService, AuthorizationService authorizationService) {
        this.specialtyService = specialtyService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<SpecialtyResponse> getAll() {
        return specialtyService.getAll().stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    public SpecialtyResponse getById(@PathVariable Long id) {
        return response(specialtyService.getById(id));
    }

    @SecurityRequirement(name = "token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyResponse create(@RequestBody SpecialtyRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        return response(specialtyService.save(new Specialty(request.getName())));
    }

    @SecurityRequirement(name = "token")
    @PutMapping("/{id}")
    public SpecialtyResponse update(@PathVariable Long id, @RequestBody SpecialtyRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        Specialty specialty = specialtyService.getById(id);
        specialty.setName(request.getName());
        return response(specialtyService.save(specialty));
    }

    @SecurityRequirement(name = "token")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        specialtyService.getById(id);
        specialtyService.delete(id);
    }

    private void validate(SpecialtyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.getName(), "name");
    }

    private SpecialtyResponse response(Specialty specialty) {
        return new SpecialtyResponse(specialty.getId(), specialty.getName());
    }
}
