package es.safareyes.gimnasio.dto.request;

import es.safareyes.gimnasio.modelos.Tipo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Validaciones de entrada del plan, complementarias a las restricciones de la base de datos.
 */
public record PlanRequest(
        @NotBlank(message = "El nombre del plan es obligatorio")
        String nombre,

        @NotNull(message = "El tipo de plan es obligatorio")
        Tipo tipo,

        @NotNull(message = "El precio mensual es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio mensual debe ser positivo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite 8 enteros y 2 decimales")
        BigDecimal precioMensual
) {
}
