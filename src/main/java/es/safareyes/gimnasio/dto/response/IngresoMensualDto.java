package es.safareyes.gimnasio.dto.response;

import java.math.BigDecimal;

/**
 * Resumen de importes de cuotas pagadas, por mes de cuota y plan historico.
 */
public record IngresoMensualDto(
        Short mes,
        String nombrePlan,
        BigDecimal total
) {
}
