package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.dto.response.IngresoMensualDto;
import es.safareyes.gimnasio.modelos.Cuota;
import es.safareyes.gimnasio.modelos.EstadoCuota;
import es.safareyes.gimnasio.dto.response.SocioMorosoResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICuotaRepository extends JpaRepository<Cuota, Long> {

    // Q1: evitar dos cuotas del mismo socio, mes y año (RN-03).
    boolean existsByIdSocio_IdAndMesAndAnyo(Long socioId, Short mes, Short anyo);

    boolean existsByIdSocio_IdAndMesAndAnyoAndEstado(Long socioId, Short mes, Short anyo, EstadoCuota estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuota c WHERE c.id = :id")
    Optional<Cuota> bloquearParaPago(@Param("id") Long id);

    long countByIdSocio_IdAndEstado(Long socioId, EstadoCuota estado);

    // Q6: historial paginado del socio (endpoint 9).
    @EntityGraph(attributePaths = "plan")
    Page<Cuota> findByIdSocio_IdOrderByAnyoDescMesDesc(Long socioId, Pageable pageable);

    @EntityGraph(attributePaths = "plan")
    Page<Cuota> findByIdSocio_Id(Long socioId, Pageable pageable);

    List<Cuota> findByMesAndAnyoAndEstado(Short mes, Short anyo, EstadoCuota estado);

    // Q2: socios con cuotas pendientes en un mes (endpoint 10).
    @Query("""
            SELECT new es.safareyes.gimnasio.dto.response.SocioMorosoResponse(
                s.id, s.dni, s.nombre, s.apellidos, c.id, c.importe)
            FROM Cuota c JOIN c.idSocio s
            WHERE c.mes = :mes AND c.anyo = :anyo AND c.estado = :estado
            ORDER BY s.apellidos, s.nombre, s.id
            """)
    List<SocioMorosoResponse> buscarSociosConCuotaPendiente(
            @Param("mes") Short mes,
            @Param("anyo") Short anyo,
            @Param("estado") EstadoCuota estado);

    // Q2 + Q5: JOIN, SUM, GROUP BY y proyeccion mediante record.
    // Agrupa por el mes al que corresponde la cuota, no por la fecha del pago.
    @Query("""
            SELECT new es.safareyes.gimnasio.dto.response.IngresoMensualDto(
                c.mes, p.nombre, SUM(c.importe))
            FROM Cuota c LEFT JOIN c.plan p
            WHERE c.anyo = :anyo AND c.estado = :estado
            GROUP BY c.mes, p.nombre
            ORDER BY c.mes, p.nombre
            """)
    List<IngresoMensualDto> resumirCuotasPagadasPorMesYPlan(
            @Param("anyo") Short anyo,
            @Param("estado") EstadoCuota estado);

    // Q3: informe SQL nativo con EXTRACT sobre la fecha real del cobro.
    // SQL nativo: pasar EstadoCuota.PAGADA.name() para comparar el VARCHAR.
    @Query(value = """
            SELECT CAST(EXTRACT(MONTH FROM c.fecha_pago) AS INTEGER) AS mes,
                   p.nombre AS plan,
                   SUM(c.importe) AS total
            FROM cuota c LEFT JOIN plan p ON p.id_plan = c.id_plan
            WHERE c.fecha_pago IS NOT NULL
              AND EXTRACT(YEAR FROM c.fecha_pago) = :anyo
              AND c.estado = :estado
            GROUP BY EXTRACT(MONTH FROM c.fecha_pago), p.nombre
            ORDER BY mes, p.nombre
            """, nativeQuery = true)
    List<Object[]> ingresosCobradosPorMesYPlan(
            @Param("anyo") int anyo,
            @Param("estado") String estado);
}
