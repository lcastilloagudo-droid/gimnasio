package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.UsuarioResponse;
import es.safareyes.gimnasio.modelos.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    @Mapping(target = "idSocio", source = "idSocio.id")
    UsuarioResponse toResponse(Usuario usuario);
}
