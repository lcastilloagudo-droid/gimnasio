package es.safareyes.gimnasio.controller;

import es.safareyes.gimnasio.dto.response.PlanResponse;
import es.safareyes.gimnasio.servicios.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/planes")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    @GetMapping
    public List<PlanResponse> listar() {
        return planService.listar();
    }
}
