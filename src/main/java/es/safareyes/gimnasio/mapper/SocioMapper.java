package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.request.SocioEstadoRequest;
import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.dto.request.SocioUpdateRequest;
import es.safareyes.gimnasio.dto.response.SocioResponse;
import es.safareyes.gimnasio.modelos.Socio;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = PlanMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface SocioMapper {

    SocioResponse toResponse(Socio socio);

    // El servicio resuelve el plan y establece fecha y estado inicial.
    // JPA genera el ID y los campos de auditoria.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plan", ignore = true)
    @Mapping(target = "fechaAlta", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    Socio toEntity(SocioRequest request);

    // PUT completo: los valores null borran telefono o matricula.
    @BeanMapping(ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "telefono", source = "telefono")
    @Mapping(target = "matricula", source = "matricula")
    void actualizar(SocioUpdateRequest request, @MappingTarget Socio socio);

    // PATCH parcial: solo cambia activo y nunca el resto de los atributos.
    @BeanMapping(ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "activo", source = "activo")
    void cambiarEstado(SocioEstadoRequest request, @MappingTarget Socio socio);
}
