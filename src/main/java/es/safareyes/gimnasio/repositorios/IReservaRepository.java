package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Estado;
import es.safareyes.gimnasio.modelos.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IReservaRepository extends JpaRepository<Reserva, Long> {

    // RN-08: impedir reservar dos veces una misma clase activa.
    boolean existsByIdSocio_IdAndIdActividad_IdAndEstado(
            Long socioId, Long actividadId, Estado estado);

    // RN-08: plazas ocupadas.
    long countByIdActividad_IdAndEstado(Long actividadId, Estado estado);

    // RN-08: un socio no puede tener mas de tres reservas futuras.
    long countByIdSocio_IdAndEstadoAndIdActividad_FechaHoraAfter(
            Long socioId, Estado estado, LocalDateTime ahora);

    Page<Reserva> findByIdSocio_IdOrderByFechaReservaDesc(Long socioId, Pageable pageable);

    @EntityGraph(attributePaths = "idActividad")
    List<Reserva> findByIdSocio_IdAndEstado(Long socioId, Estado estado);
}
