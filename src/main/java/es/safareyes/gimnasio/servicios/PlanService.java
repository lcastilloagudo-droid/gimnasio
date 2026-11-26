package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.response.PlanResponse;
import es.safareyes.gimnasio.mapper.PlanMapper;
import es.safareyes.gimnasio.repositorios.IPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanService {
    private final IPlanRepository planRepository;
    private final PlanMapper planMapper;

    public List<PlanResponse> listar() {
        return planMapper.toResponseList(planRepository.findAllByOrderByPrecioMensualAsc());
    }
}
