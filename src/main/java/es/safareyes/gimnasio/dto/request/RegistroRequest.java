package es.safareyes.gimnasio.dto.request;

import es.safareyes.gimnasio.validacion.DniValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @DniValido
        String dni,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 100)
        String nomUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
