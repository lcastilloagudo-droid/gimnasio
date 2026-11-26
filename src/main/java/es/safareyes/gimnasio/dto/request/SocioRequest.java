package es.safareyes.gimnasio.dto.request;

import es.safareyes.gimnasio.validacion.DniValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Validaciones de formato para el alta de socios.
 * Las reglas de negocio (DNI/email duplicado, estado, plan) van en el servicio.
 */
public record SocioRequest(

        @NotBlank(message = "El DNI es obligatorio")
        @DniValido
        String dni,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150)
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String telefono,

        @Pattern(
                regexp = "[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}",
                message = "La matricula debe tener formato 1234BCD"
        )
        String matricula,

        @NotNull(message = "Debes indicar un plan")
        @Positive(message = "El identificador del plan debe ser positivo")
        Long idPlan
) {
}
