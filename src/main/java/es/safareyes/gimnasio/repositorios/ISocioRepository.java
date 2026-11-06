package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Socio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISocioRepository extends JpaRepository<Socio, Long> {
}
