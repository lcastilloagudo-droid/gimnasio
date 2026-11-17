package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.dto.response.IngresoMensualDto;
import es.safareyes.gimnasio.modelos.Cuota;
import es.safareyes.gimnasio.modelos.Socio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ICuotaRepository extends JpaRepository<Cuota, Long> {

    // Q1: evitar dos cuotas del mismo socio, mes y año (RN-03).
    boolean existsByIdSocio_IdAndMesAndAnyo(Long socioId, Short mes, Short anyo);

    long countByIdSocio_IdAndEstado(Long socioId, String estado);

    // Q6: historial paginado del socio (endpoint 9).
    Page<Cuota> findByIdSocio_IdOrderByAnyoDescMesDesc(Long socioId, Pageable pageable);

    List<Cuota> findByMesAndAnyoAndEstado(Short mes, Short anyo, String estado);

    // Q2: socios con cuotas pendientes en un mes (endpoint 10).
    @Query("""
            SELECT DISTINCT s
            FROM Cuota c JOIN c.idSocio s
            WHERE c.mes = :mes AND c.anyo = :anyo AND c.estado = :estado
            ORDER BY s.apellidos, s.nombre
            """)
    List<Socio> buscarSociosConCuotaPendiente(
            @Param("mes") Short mes,
            @Param("anyo") Short anyo,
            @Param("estado") String estado);

    // Q2 + Q5: JOIN, SUM, GROUP BY y proyeccion mediante record.
    // Agrupa por el mes al que corresponde la cuota, no por la fecha del pago.
    @Query("""
            SELECT new es.safareyes.gimnasio.dto.response.IngresoMensualDto(
                c.mes, p.nombre, SUM(c.importe))
            FROM Cuota c JOIN c.plan p
            WHERE c.anyo = :anyo AND c.estado = :estado
            GROUP BY c.mes, p.nombre
            ORDER BY c.mes, p.nombre
            """)
    List<IngresoMensualDto> resumirCuotasPagadasPorMesYPlan(
            @Param("anyo") Short anyo,
            @Param("estado") String estado);

    // Q3: informe SQL nativo con EXTRACT sobre la fecha real del cobro.
    @Query(value = """
            SELECT CAST(EXTRACT(MONTH FROM c.fecha_pago) AS INTEGER) AS mes,
                   p.nombre AS plan,
                   SUM(c.importe) AS total
            FROM cuota c JOIN plan p ON p.id_plan = c.id_plan
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
