package ar.edu.unnoba.pm2026.user.controller;

import ar.edu.unnoba.pm2026.auth.dto.AuthenticationRequest;
import ar.edu.unnoba.pm2026.auth.service.AuthenticationService;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.web.ResourceLocation;
import ar.edu.unnoba.pm2026.user.dto.AdminAccountResponse;
import ar.edu.unnoba.pm2026.user.dto.CreateAdminRequest;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.service.AdministratorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TP6 (#39): login y cuentas de administrador bajo /admin. Convive con /administrator/auth y
// /api/administrators, que sigue usando el front.
@RestController
@RequestMapping("/admin")
public class AdminAccountController {

    private final AdministratorService administratorService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    public AdminAccountController(AdministratorService administratorService,
            AuthenticationService authenticationService, AuthorizationService authorizationService) {
        this.administratorService = administratorService;
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
    }

    // Login separado del administrador: {"token": "Bearer ..."} o 401
    @PostMapping(value = "/auth", produces = "application/json")
    public Map<String, String> authentication(@RequestBody @Valid AuthenticationRequest request) {
        Administrator credentials = new Administrator(null, null, request.email(), request.password());
        return Map.of("token", authenticationService.authenticate(credentials));
    }

    @SecurityRequirement(name = "token")
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AdminAccountResponse> create(@RequestBody @Valid CreateAdminRequest request,
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        Administrator administrator = new Administrator(request.getName(), request.getLastName(),
                request.getEmail(), request.getPassword());
        AdminAccountResponse created = AdminAccountResponse.from(administratorService.create(administrator));
        return ResponseEntity.created(ResourceLocation.forId(created.id())).body(created);
    }

    @SecurityRequirement(name = "token")
    @GetMapping("/accounts")
    public List<AdminAccountResponse> getAll(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.authorize(token, Role.ADMINISTRATOR);
        return administratorService.getAll().stream().map(AdminAccountResponse::from).toList();
    }

    // 404 si no existe; 409 si el administrador intenta borrarse a si mismo
    @SecurityRequirement(name = "token")
    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser actor = authorizationService.authorize(token, Role.ADMINISTRATOR);
        administratorService.delete(id, actor);
    }
}
