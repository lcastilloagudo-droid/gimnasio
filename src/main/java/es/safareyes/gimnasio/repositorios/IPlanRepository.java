package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IPlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Plan> findAllByOrderByPrecioMensualAsc();
}
