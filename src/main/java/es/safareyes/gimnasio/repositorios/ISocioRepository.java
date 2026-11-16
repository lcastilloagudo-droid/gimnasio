package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Socio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ISocioRepository extends JpaRepository<Socio, Long>, JpaSpecificationExecutor<Socio> {

    // Q1: consultas derivadas. Spring Data genera el SQL a partir del nombre.
    Optional<Socio> findByDniIgnoreCase(String dni);

    boolean existsByDniIgnoreCase(String dni);

    boolean existsByEmailIgnoreCase(String email);

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

    // Q5: proyeccion por interfaz; no carga entidades enteras.
    interface SocioResumen {
        Long getId();
        String getDni();
        String getNombre();
        String getApellidos();
        Boolean getActivo();
    }

    Page<SocioResumen> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Q8: cargar plan de una vez en listados; evita consultas extra por socio.
    @EntityGraph(attributePaths = "plan")
    Page<Socio> findByActivoTrue(Pageable pageable);
}
