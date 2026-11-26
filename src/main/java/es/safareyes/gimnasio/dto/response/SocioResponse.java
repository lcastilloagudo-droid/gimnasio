package es.safareyes.gimnasio.dto.response;

import java.time.LocalDate;

public record SocioResponse(
        Long id,
        String dni,
        String nombre,
        String apellidos,
        String email,
        String telefono,
        LocalDate fechaAlta,
        Boolean activo,
        String matricula,
        PlanResponse plan
) {
}
