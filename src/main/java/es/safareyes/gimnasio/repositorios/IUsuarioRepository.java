package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUsuarioRepository extends JpaRepository<Usuario, Short> {

}
