package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.ReservaResponse;
import es.safareyes.gimnasio.modelos.Reserva;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservaMapper {
    @Mapping(target = "idActividad", source = "idActividad.id")
    ReservaResponse toResponse(Reserva reserva);

    List<ReservaResponse> toResponseList(List<Reserva> reservas);
}
