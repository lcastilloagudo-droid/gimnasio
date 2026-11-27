package es.safareyes.gimnasio.controller;

import es.safareyes.gimnasio.dto.request.PagoCuotaRequest;
import es.safareyes.gimnasio.dto.request.PeriodoCuotasRequest;
import es.safareyes.gimnasio.dto.response.CuotaResponse;
import es.safareyes.gimnasio.dto.response.GeneracionCuotasResponse;
import es.safareyes.gimnasio.dto.response.SocioMorosoResponse;
import es.safareyes.gimnasio.servicios.CuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cuotas")
@RequiredArgsConstructor
@Slf4j
public class CuotaController {
    private final CuotaService cuotaService;

    @PostMapping("/generar")
    public GeneracionCuotasResponse generar(@Valid @ModelAttribute PeriodoCuotasRequest periodo) {
        return cuotaService.generar(periodo);
    }

    @PostMapping("/pagar")
    public CuotaResponse pagar(@Valid @RequestBody PagoCuotaRequest request) {
        return cuotaService.pagar(request);
    }

    @GetMapping("/pendientes")
    public List<SocioMorosoResponse> pendientes(@Valid @ModelAttribute PeriodoCuotasRequest periodo) {
        log.debug("Consulta de cuotas pendientes del periodo {}/{}", periodo.mes(), periodo.anio());
        return cuotaService.pendientes(periodo);
    }
}
