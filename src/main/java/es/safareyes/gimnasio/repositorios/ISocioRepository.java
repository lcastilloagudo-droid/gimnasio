package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Socio;
import es.safareyes.gimnasio.modelos.EstadoCuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ISocioRepository extends JpaRepository<Socio, Long>, JpaSpecificationExecutor<Socio> {

    // Q1: consultas derivadas. Spring Data genera el SQL a partir del nombre.
    Optional<Socio> findByDniIgnoreCase(String dni);

    boolean existsByDniIgnoreCase(String dni);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    // Carga el plan del socio tambien al aplicar filtros dinamicos paginados.
    @Override
    @EntityGraph(attributePaths = "plan")
    Page<Socio> findAll(Specification<Socio> spec, Pageable pageable);

    Page<Socio> findByActivo(Boolean activo, Pageable pageable);

    List<Socio> findByFechaAltaBetweenOrderByFechaAltaDesc(LocalDate desde, LocalDate hasta);

    long countByActivoTrue();

    // Q2: JPQL con JOIN. Incluye los socios sin plan asignado.
    @Query("""
            SELECT s FROM Socio s
            LEFT JOIN FETCH s.plan
            WHERE s.id = :id
            """)
    Optional<Socio> buscarConPlan(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Socio s WHERE s.id = :id")
    Optional<Socio> bloquearPorId(@Param("id") Long id);

    // Q5: proyeccion por interfaz; no carga entidades enteras.
    interface SocioResumen {
        Long getId();
        String getDni();
        String getNombre();
        String getApellidos();
        Boolean getActivo();
    }

    Page<SocioResumen> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Consulta paginada que carga el plan N:1 en una sola operacion.
    @EntityGraph(attributePaths = "plan")
    Page<Socio> findByActivoTrue(Pageable pageable);

    // Serializa la generación mensual y espera a las escrituras de SocioService.
    @Modifying
    @Query(value = "LOCK TABLE socio IN EXCLUSIVE MODE", nativeQuery = true)
    void bloquearParaGenerarCuotas();

    // Q4: RN-04, desactivar en bloque socios con al menos N cuotas pendientes.
    // Se ejecuta dentro de una transaccion; devuelve cuantas filas modifica.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE Socio s SET s.activo = false, s.actualizadoEn = :actualizadoEn
            WHERE s.activo = true AND s.id IN (
                SELECT c.idSocio.id
                FROM Cuota c
                WHERE c.estado = :estadoPendiente
                  AND (c.mes <> :mes OR c.anyo <> :anyo)
                GROUP BY c.idSocio.id
                HAVING COUNT(c.id) >= :minimoPendientes
            )
            """)
    int desactivarSociosMorosos(
            @Param("estadoPendiente") EstadoCuota estadoPendiente,
            @Param("minimoPendientes") long minimoPendientes,
            @Param("mes") short mes,
            @Param("anyo") short anyo,
            @Param("actualizadoEn") LocalDateTime actualizadoEn);
}
