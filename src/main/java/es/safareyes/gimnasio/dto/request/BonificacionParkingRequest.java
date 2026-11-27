package es.safareyes.gimnasio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BonificacionParkingRequest(
        @NotBlank @Pattern(regexp = "[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}") String matricula,
        @NotBlank @Pattern(regexp = "FITZONE") String origen,
        @NotBlank String referencia
) {}
