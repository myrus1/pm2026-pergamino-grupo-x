package ar.edu.unnoba.pm2026.user.dto;

import ar.edu.unnoba.pm2026.user.model.Administrator;

// TP6: un administrador en /admin/accounts (id, name, lastName, email), sin password
public record AdminAccountResponse(Long id, String name, String lastName, String email) {

    public static AdminAccountResponse from(Administrator administrator) {
        return new AdminAccountResponse(administrator.getId(), administrator.getFirstName(),
                administrator.getLastName(), administrator.getEmail());
    }
}
