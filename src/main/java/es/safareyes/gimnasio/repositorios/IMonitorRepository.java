package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IMonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findAllByOrderByApellidosAscNombreAsc();

    List<Monitor> findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String nombre, String apellidos);
}
