package es.safareyes.gimnasio.dto.response;

import java.time.LocalDateTime;

public record ActividadResponse(
        Long id,
        String nombre,
        LocalDateTime fechaHora,
        Integer duracion,
        String monitor,
        String sala,
        Integer aforoMax,
        int plazasLibres
) {}
