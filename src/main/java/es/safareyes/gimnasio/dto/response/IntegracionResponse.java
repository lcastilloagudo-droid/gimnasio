package es.safareyes.gimnasio.dto.response;

public record IntegracionResponse(
        String servicio,
        Estado estado,
        String detalle
) {
    public enum Estado {
        OK,
        NO_APLICA,
        RECHAZADO,
        NO_DISPONIBLE
    }
}
