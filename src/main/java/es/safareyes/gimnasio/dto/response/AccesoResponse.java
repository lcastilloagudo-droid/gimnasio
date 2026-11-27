package es.safareyes.gimnasio.dto.response;

import es.safareyes.gimnasio.modelos.MotivoDenegacion;
import es.safareyes.gimnasio.modelos.ResultadoAcceso;

import java.time.LocalDateTime;

public record AccesoResponse(
        Long id,
        ResultadoAcceso resultado,
        MotivoDenegacion motivoDenegacion,
        LocalDateTime fechaHoraEntrada,
        LocalDateTime fechaHoraSalida,
        IntegracionResponse integracion
) {}
