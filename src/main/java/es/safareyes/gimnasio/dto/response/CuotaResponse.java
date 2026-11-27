package es.safareyes.gimnasio.dto.response;

import es.safareyes.gimnasio.modelos.EstadoCuota;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaResponse(
        Long id,
        Long idSocio,
        PlanResponse plan,
        Short mes,
        Short anio,
        BigDecimal importe,
        EstadoCuota estado,
        LocalDate fechaPago
) {}
