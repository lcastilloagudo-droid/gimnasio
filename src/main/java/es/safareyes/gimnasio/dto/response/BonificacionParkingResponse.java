package es.safareyes.gimnasio.dto.response;

import java.math.BigDecimal;

public record BonificacionParkingResponse(
        String matricula,
        Long ticketId,
        String promocion,
        int minutosAplicados,
        BigDecimal importeActual
) {}
