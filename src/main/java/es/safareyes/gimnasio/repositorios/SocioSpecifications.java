package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Socio;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filtros combinables para el listado paginado de socios (RF-02, Q7).
 * Un parametro null significa que ese filtro no se aplica.
 */
public final class SocioSpecifications {

    private SocioSpecifications() {
    }

    public static Specification<Socio> conFiltros(
            Boolean activo,
            Long idPlan,
            String texto,
            LocalDate fechaAltaDesde,
            LocalDate fechaAltaHasta
    ) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }

            if (idPlan != null) {
                condiciones.add(cb.equal(root.get("plan").get("id"), idPlan));
            }

            if (texto != null && !texto.isBlank()) {
                String patron = "%" + escaparLike(texto.trim().toLowerCase(Locale.ROOT)) + "%";

                Predicate porNombre = cb.like(cb.lower(root.get("nombre")), patron, '\\');
                Predicate porApellidos = cb.like(cb.lower(root.get("apellidos")), patron, '\\');
                Predicate porDni = cb.like(cb.lower(root.get("dni")), patron, '\\');

                condiciones.add(cb.or(porNombre, porApellidos, porDni));
            }

            if (fechaAltaDesde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaAlta"), fechaAltaDesde));
            }

            if (fechaAltaHasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fechaAlta"), fechaAltaHasta));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    // Hace que %, _ y la barra invertida se busquen literalmente.
    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
