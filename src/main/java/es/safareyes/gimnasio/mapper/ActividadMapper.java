package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.ActividadResponse;
import es.safareyes.gimnasio.modelos.Actividad;
import es.safareyes.gimnasio.modelos.Monitor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActividadMapper {
    @Mapping(target = "monitor", source = "actividad.idMonitor")
    @Mapping(target = "plazasLibres", source = "plazasLibres")
    ActividadResponse toResponse(Actividad actividad, int plazasLibres);

    default String nombreCompleto(Monitor monitor) {
        return monitor == null ? null : monitor.getNombre() + " " + monitor.getApellidos();
    }
}
