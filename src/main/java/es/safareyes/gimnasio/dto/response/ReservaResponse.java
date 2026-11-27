package es.safareyes.gimnasio.dto.response;

import es.safareyes.gimnasio.modelos.Estado;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long idActividad,
        LocalDateTime fechaReserva,
        Estado estado
) {}
