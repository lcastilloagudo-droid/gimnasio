package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IAccesoRepository extends JpaRepository<Acceso, Long> {

    // Q1: ultimo intento de acceso de un socio.
    Optional<Acceso> findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc(Long socioId);

    // RN-05/RN-07: localizar una entrada permitida que aun no tiene salida.
    Optional<Acceso> findFirstByIdSocio_IdAndTipoResultadoAndFechaHoraSalidaIsNullOrderByFechaHoraEntradaDesc(
            Long socioId, String resultado);

    boolean existsByIdSocio_IdAndTipoResultadoAndFechaHoraSalidaIsNull(
            Long socioId, String resultado);

    // Q2: el aforo cuenta solo entradas permitidas de hoy sin salida registrada.
    @Query("""
            SELECT COUNT(a)
            FROM Acceso a
            WHERE a.tipoResultado = :permitido
              AND a.fechaHoraSalida IS NULL
              AND a.fechaHoraEntrada >= :desde
              AND a.fechaHoraEntrada < :hasta
            """)
    long contarPersonasDentro(
            @Param("permitido") String permitido,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Q3: horas punta, agrupando los accesos por horas (PostgreSQL).
    @Query(value = """
            SELECT DATE_TRUNC('hour', a.fecha_hora_entrada) AS hora,
                   COUNT(*) AS entradas
            FROM acceso a
            WHERE a.tipo_resultado = :permitido
              AND a.fecha_hora_entrada >= :desde
              AND a.fecha_hora_entrada < :hasta
            GROUP BY DATE_TRUNC('hour', a.fecha_hora_entrada)
            ORDER BY hora
            """, nativeQuery = true)
    List<Object[]> contarEntradasPorHora(
            @Param("permitido") String permitido,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
