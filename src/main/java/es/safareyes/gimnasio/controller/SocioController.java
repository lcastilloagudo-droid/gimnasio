package es.safareyes.gimnasio.controller;

import es.safareyes.gimnasio.dto.request.SocioEstadoRequest;
import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.dto.request.SocioUpdateRequest;
import es.safareyes.gimnasio.dto.response.SocioResponse;
import es.safareyes.gimnasio.servicios.SocioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/socios")
@RequiredArgsConstructor
@Slf4j
public class SocioController {
    private final SocioService socioService;

    @GetMapping
    public Page<SocioResponse> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) @Positive Long idPlan,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaAltaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaAltaHasta,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        log.debug("Consulta paginada de socios");
        return socioService.listar(activo, idPlan, texto, fechaAltaDesde, fechaAltaHasta, pageable);
    }

    @GetMapping("/{id}")
    public SocioResponse getSocioById(@PathVariable @Positive Long id) {
        return socioService.getSocioById(id);
    }

    @PostMapping
    public ResponseEntity<SocioResponse> createSocio(@Valid @RequestBody SocioRequest request) {
        SocioResponse creado = socioService.createSocio(request);
        URI location = URI.create("/api/v1/socios/" + creado.id());
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public SocioResponse actualizar(@PathVariable @Positive Long id,
                                    @Valid @RequestBody SocioUpdateRequest request) {
        return socioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public SocioResponse cambiarEstado(@PathVariable @Positive Long id,
                                       @Valid @RequestBody SocioEstadoRequest request) {
        return socioService.cambiarEstado(id, request);
    }
}
