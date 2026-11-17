package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Actividad;
import es.safareyes.gimnasio.modelos.Estado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IActividadRepository extends JpaRepository<Actividad, Long> {

    // Q1: actividades del dia: [desde, hasta).
    List<Actividad> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
            LocalDateTime desde, LocalDateTime hasta);

    // Q8: cargar de una vez monitor y coleccion de reservas de cada actividad.
    // Sin Pageable: la paginacion de un JOIN FETCH de colecciones no es segura.
    @EntityGraph(attributePaths = {"idMonitor", "reservas"})
    List<Actividad> findDistinctByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
            LocalDateTime desde, LocalDateTime hasta);

    // Q2: total de reservas con el estado indicado para cada actividad del dia.
    @Query("""
            SELECT a.id, COUNT(r.id)
            FROM Actividad a
            LEFT JOIN a.reservas r ON r.estado = :estado
            WHERE a.fechaHora >= :desde AND a.fechaHora < :hasta
            GROUP BY a.id
            ORDER BY a.id
            """)
    List<Object[]> contarReservasPorActividad(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estado") Estado estado);

    // La futura logica de reservas puede bloquear la actividad al comprobar aforo.
    // Llamar dentro de un servicio @Transactional.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Actividad a WHERE a.id = :id")
    Optional<Actividad> bloquearParaReserva(@Param("id") Long id);
}
