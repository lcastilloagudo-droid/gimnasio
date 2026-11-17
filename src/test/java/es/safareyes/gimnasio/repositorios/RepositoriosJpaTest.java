package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.modelos.Actividad;
import es.safareyes.gimnasio.modelos.Estado;
import es.safareyes.gimnasio.modelos.Socio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 2: pruebas de las consultas JPA (Q1-Q8) sobre los datos de sql/data.sql.
 * Las transacciones de @DataJpaTest se revierten al terminar cada prueba.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoriosJpaTest {

    @Autowired private ISocioRepository socios;
    @Autowired private IPlanRepository planes;
    @Autowired private ICuotaRepository cuotas;
    @Autowired private IAccesoRepository accesos;
    @Autowired private IActividadRepository actividades;
    @Autowired private IReservaRepository reservas;
    @Autowired private IMonitorRepository monitores;
    @Autowired private IUsuarioRepository usuarios;

    @Value("${spring.datasource.hikari.schema}")
    private String esquema;

    private Socio ana() {
        assertThat(esquema).isEqualTo("gimnasio_test");
        return socios.findByDniIgnoreCase("00000001R").orElseThrow();
    }

    private Actividad spinningCompleto() {
        return actividades.findAll().stream()
                .filter(a -> a.getNombre().equals("Spinning") && a.getAforoMax() == 2)
                .findFirst().orElseThrow();
    }

    @Test
    void q1_consultasDerivadas() {
        Long idAna = ana().getId();

        assertThat(socios.existsByDniIgnoreCase("00000001r")).isTrue();
        assertThat(socios.existsByEmailIgnoreCase("SOCIO01@EJEMPLO.TEST")).isTrue();
        assertThat(socios.countByActivoTrue()).isEqualTo(17);
        assertThat(planes.findByNombreIgnoreCase("premium")).isPresent();
        assertThat(monitores.findAllByOrderByApellidosAscNombreAsc()).hasSize(4);
        assertThat(usuarios.findByNomUsuarioIgnoreCase("ADMIN_FITZONE")).isPresent();
        assertThat(accesos.findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc(idAna)).isPresent();
        assertThat(reservas.countByIdActividad_IdAndEstado(
                spinningCompleto().getId(), Estado.ACTIVA)).isEqualTo(2);
    }

    @Test
    void q2_consultasJpql() {
        Socio socio = ana();
        assertThat(socios.buscarConPlan(socio.getId()).orElseThrow().getPlan())
                .isNotNull();

        var primeraCuota = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                socio.getId(), PageRequest.of(0, 1)).getContent().get(0);

        assertThat(cuotas.buscarSociosConCuotaPendiente(
                primeraCuota.getMes(), primeraCuota.getAnyo(), "PENDIENTE")).hasSize(6);
        assertThat(cuotas.resumirCuotasPagadasPorMesYPlan(
                primeraCuota.getAnyo(), "PAGADA")).isNotEmpty();

        Actividad actividad = spinningCompleto();
        LocalDate fecha = actividad.getFechaHora().toLocalDate();
        assertThat(actividades.contarReservasPorActividad(
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay(), Estado.ACTIVA))
                .isNotEmpty();
    }

    @Test
    void q3_consultasNativas() {
        Long idAna = ana().getId();
        var cuotaPagada = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                idAna, PageRequest.of(0, 1)).getContent().get(0);

        assertThat(cuotas.ingresosCobradosPorMesYPlan(
                cuotaPagada.getFechaPago().getYear(), "PAGADA")).isNotEmpty();

        LocalDate fecha = accesos.findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc(
                idAna).orElseThrow().getFechaHoraEntrada().toLocalDate();
        LocalDateTime desde = fecha.atStartOfDay();

        assertThat(accesos.contarEntradasPorHora(
                "PERMITIDO", desde, fecha.plusDays(1).atStartOfDay())).isNotEmpty();
    }

    @Test
    void q4_modificacionMasiva() {
        assertThat(ana()).isNotNull(); // Comprueba el esquema de pruebas.
        Socio moroso = socios.findByDniIgnoreCase("00000015S").orElseThrow();
        assertThat(moroso.getActivo()).isTrue();

        assertThat(socios.desactivarSociosMorosos("PENDIENTE", 2)).isEqualTo(1);
        assertThat(socios.findById(moroso.getId()).orElseThrow().getActivo()).isFalse();
        // @DataJpaTest revierte el UPDATE al terminar.
    }

    @Test
    void q5_proyecciones() {
        Socio socio = ana();
        assertThat(socios.findByNombreContainingIgnoreCase(
                "ana", PageRequest.of(0, 10)).getContent())
                .extracting(ISocioRepository.SocioResumen::getNombre)
                .contains("Ana");

        var cuota = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                socio.getId(), PageRequest.of(0, 1)).getContent().get(0);
        assertThat(cuotas.resumirCuotasPagadasPorMesYPlan(
                cuota.getAnyo(), "PAGADA")).isNotEmpty();
    }

    @Test
    void q6_paginacion() {
        Long idAna = ana().getId();
        assertThat(socios.findByActivo(true, PageRequest.of(0, 5)).getContent())
                .hasSize(5);
        assertThat(cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                idAna, PageRequest.of(0, 2)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void q7_filtrosDinamicos() {
        Socio socio = ana();

        var filtro = SocioSpecifications.conFiltros(
                true, socio.getPlan().getId(), "Lopez",
                socio.getFechaAlta().minusDays(1),
                socio.getFechaAlta().plusDays(1));

        assertThat(socios.findAll(filtro, PageRequest.of(0, 10)).getContent())
                .extracting(Socio::getId)
                .contains(socio.getId());
    }

    @Test
    void q8_evitarNMasUno() {
        assertThat(ana()).isNotNull(); // Comprueba el esquema de pruebas.
        Actividad completa = spinningCompleto();
        LocalDate fecha = completa.getFechaHora().toLocalDate();

        var resultado = actividades
                .findDistinctByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                        fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());

        assertThat(resultado).extracting(Actividad::getId).doesNotHaveDuplicates();

        Actividad cargada = resultado.stream()
                .filter(a -> a.getId().equals(completa.getId()))
                .findFirst().orElseThrow();

        assertThat(cargada.getIdMonitor()).isNotNull();
        assertThat(cargada.getReservas()).filteredOn(r -> r.getEstado() == Estado.ACTIVA)
                .hasSize(2);
    }
}
