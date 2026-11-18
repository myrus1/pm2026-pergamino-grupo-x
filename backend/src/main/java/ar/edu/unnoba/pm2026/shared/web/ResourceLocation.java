package ar.edu.unnoba.pm2026.shared.web;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ResourceLocation {

    private ResourceLocation() {
    }

    public static URI forId(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    // Para las altas del TP6 cuyo recurso se consulta en otra ruta (p. ej. POST /admin/accounts -> /api/administrators/{id})
    public static URI forPath(String template, Object... variables) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(template)
                .buildAndExpand(variables)
                .toUri();
    }
}
