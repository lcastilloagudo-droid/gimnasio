package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends JpaRepository<Usuario, Long> {

    // Datos necesarios para el futuro registro y login con JWT.
    Optional<Usuario> findByNomUsuarioIgnoreCase(String nomUsuario);

    boolean existsByNomUsuarioIgnoreCase(String nomUsuario);

    Optional<Usuario> findByIdSocio_Id(Long socioId);
}
