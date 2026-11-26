package es.safareyes.gimnasio.dto.request;

import jakarta.validation.constraints.NotNull;

public record SocioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo
) {
}
