package es.safareyes.gimnasio.dto.response;

import es.safareyes.gimnasio.modelos.Tipo;
import java.math.BigDecimal;

public record PlanResponse(
        Long id,
        String nombre,
        Tipo tipo,
        BigDecimal precioMensual
) {
}
