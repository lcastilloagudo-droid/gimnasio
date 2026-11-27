package es.safareyes.gimnasio.dto.response;

import es.safareyes.gimnasio.modelos.Tipo;

public record EstadoSocioIntegracionResponse(
        String dni,
        boolean socioActivo,
        boolean cuotaAlCorriente,
        Tipo plan
) {}
