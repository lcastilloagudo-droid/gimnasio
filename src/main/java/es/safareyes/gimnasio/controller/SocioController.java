package es.safareyes.gimnasio.controller;

import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.servicios.SocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/socios")
@RequiredArgsConstructor
public class SocioController {
    private final SocioService socioService;

    @GetMapping("/{id}")
    public ResponseEntity<SocioRequest> getSocioById(@PathVariable("id") Long id) {
        return ResponseEntity.of(socioService.getSociosById(id));

    }
    @PostMapping("/crear")
    public ResponseEntity<SocioRequest> createSocio(@RequestBody SocioRequest socioRequest) {
        return ResponseEntity.ok(socioService.createSocio(socioRequest));
    }
}
