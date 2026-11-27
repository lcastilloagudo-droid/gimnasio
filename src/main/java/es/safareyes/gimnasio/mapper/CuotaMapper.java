package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.CuotaResponse;
import es.safareyes.gimnasio.modelos.Cuota;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = PlanMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CuotaMapper {
    @Mapping(target = "idSocio", source = "idSocio.id")
    @Mapping(target = "anio", source = "anyo")
    CuotaResponse toResponse(Cuota cuota);

    List<CuotaResponse> toResponseList(List<Cuota> cuotas);
}
