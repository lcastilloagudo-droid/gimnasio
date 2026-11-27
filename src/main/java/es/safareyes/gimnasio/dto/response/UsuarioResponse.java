package es.safareyes.gimnasio.dto.response;

import es.safareyes.gimnasio.modelos.Rol;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        Rol rol
) {}
