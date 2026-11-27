package es.safareyes.gimnasio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** PUT: sustituye el contacto, el plan y la matricula. Los valores null eliminan los datos opcionales. */
public record SocioUpdateRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String telefono,

        @Pattern(regexp = "[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}",
                message = "La matricula debe tener formato 1234BCD")
        String matricula,

        @Positive(message = "El identificador del plan debe ser positivo")
        Long idPlan
) {
}
