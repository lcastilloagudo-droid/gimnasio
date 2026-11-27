package es.safareyes.gimnasio.dto.request;

import es.safareyes.gimnasio.validacion.DniValido;
import jakarta.validation.constraints.NotBlank;

public record AccesoRequest(
        @NotBlank(message = "El DNI es obligatorio") @DniValido String dni
) {}
