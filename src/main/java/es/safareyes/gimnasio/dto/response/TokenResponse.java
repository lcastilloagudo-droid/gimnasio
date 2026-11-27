package es.safareyes.gimnasio.dto.response;

public record TokenResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String nomUsuario,
        String rol
) {}
