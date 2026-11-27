package es.safareyes.gimnasio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 100)
        String nomUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
