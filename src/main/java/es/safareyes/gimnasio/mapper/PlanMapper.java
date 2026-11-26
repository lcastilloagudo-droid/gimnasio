package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.PlanResponse;
import es.safareyes.gimnasio.modelos.Plan;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanMapper {
    PlanResponse toResponse(Plan plan);
    List<PlanResponse> toResponseList(List<Plan> planes);
}
