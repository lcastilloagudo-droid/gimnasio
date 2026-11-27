package es.safareyes.gimnasio.dto.response;

import java.math.BigDecimal;

public record SocioMorosoResponse(
        Long idSocio,
        String dni,
        String nombre,
        String apellidos,
        Long idCuota,
        BigDecimal importe
) {}
