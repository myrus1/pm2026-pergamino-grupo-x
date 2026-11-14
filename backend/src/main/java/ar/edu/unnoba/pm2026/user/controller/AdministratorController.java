package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.shared.validation.RequestValidator;
import static ar.edu.unnoba.pm2026.shared.validation.RequestValidator.requireField;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.user.dto.AdministratorRequest;
import ar.edu.unnoba.pm2026.user.dto.AdministratorResponse;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
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

// API REST de administradores: /api/administrators (todo es de administrador)
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/api/administrators")
public class AdministratorController {

    private final AdministratorService administratorService;
    private final AuthorizationService authorizationService;

    public AdministratorController(AdministratorService administratorService,
            AuthorizationService authorizationService) {
        this.administratorService = administratorService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<AdministratorResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return administratorService.getAll().stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    public AdministratorResponse getById(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return response(administratorService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdministratorResponse create(@RequestBody AdministratorRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        Administrator administrator = new Administrator(request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPassword());
        return response(administratorService.create(administrator));
    }

    @PutMapping("/{id}")
    public AdministratorResponse update(@PathVariable Long id, @RequestBody AdministratorRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        validate(request);
        Administrator administrator = administratorService.getById(id);
        administrator.setFirstName(request.getFirstName());
        administrator.setLastName(request.getLastName());
        administrator.setEmail(request.getEmail());
        administrator.setPassword(request.getPassword());
        return response(administratorService.update(administrator));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        administratorService.getById(id);
        administratorService.delete(id);
    }

    private void validate(AdministratorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        requireField(request.getFirstName(), "firstName");
        requireField(request.getLastName(), "lastName");
        requireField(request.getEmail(), "email");
        requireField(request.getPassword(), "password");
    }

    private AdministratorResponse response(Administrator administrator) {
        return new AdministratorResponse(administrator.getId(), administrator.getFirstName(),
                administrator.getLastName(), administrator.getEmail());
    }
}
