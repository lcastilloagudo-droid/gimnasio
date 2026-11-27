package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.AccesoResponse;
import es.safareyes.gimnasio.dto.response.IntegracionResponse;
import es.safareyes.gimnasio.modelos.Acceso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccesoMapper {
    @Mapping(target = "resultado", source = "acceso.tipoResultado")
    @Mapping(target = "integracion", source = "integracion")
    AccesoResponse toResponse(Acceso acceso, IntegracionResponse integracion);
}
