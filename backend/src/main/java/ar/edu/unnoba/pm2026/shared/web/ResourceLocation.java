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
}
