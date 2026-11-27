package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.dto.request.SocioUpdateRequest;
import es.safareyes.gimnasio.mapper.PlanMapperImpl;
import es.safareyes.gimnasio.mapper.SocioMapperImpl;
import es.safareyes.gimnasio.modelos.Actividad;
import es.safareyes.gimnasio.modelos.Cuota;
import es.safareyes.gimnasio.modelos.Reserva;
import es.safareyes.gimnasio.servicios.SocioService;
import jakarta.persistence.EntityManager;
import es.safareyes.gimnasio.modelos.Estado;
import es.safareyes.gimnasio.modelos.EstadoCuota;
import es.safareyes.gimnasio.modelos.MotivoDenegacion;
import es.safareyes.gimnasio.modelos.ResultadoAcceso;
import es.safareyes.gimnasio.modelos.Tipo;
import es.safareyes.gimnasio.modelos.Socio;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 2: pruebas de las consultas JPA (Q1-Q8) sobre los datos de sql/data.sql.
 * Las transacciones de @DataJpaTest se revierten al terminar cada prueba.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SocioService.class, SocioMapperImpl.class, PlanMapperImpl.class})
class RepositoriosJpaTest {

    @Autowired private ISocioRepository socios;
    @Autowired private IPlanRepository planes;
    @Autowired private ICuotaRepository cuotas;
    @Autowired private IAccesoRepository accesos;
    @Autowired private IActividadRepository actividades;
    @Autowired private IReservaRepository reservas;
    @Autowired private IMonitorRepository monitores;
    @Autowired private IUsuarioRepository usuarios;
    @Autowired private SocioService socioService;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

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
        assertThat(planes.findByNombreIgnoreCase("premium").orElseThrow().getTipo())
                .isEqualTo(Tipo.PREMIUM);
        assertThat(monitores.findAllByOrderByApellidosAscNombreAsc()).hasSize(4);
        assertThat(usuarios.findByNomUsuarioIgnoreCase("ADMIN_FITZONE")).isPresent();
        var accesoAna = accesos.findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc(idAna).orElseThrow();
        assertThat(accesoAna.getTipoResultado()).isEqualTo(ResultadoAcceso.PERMITIDO);
        assertThat(accesoAna.getMotivoDenegacion()).isNull();
        Long idDaniel = socios.findByDniIgnoreCase("00000006Y").orElseThrow().getId();
        var accesoDaniel = accesos.findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc(idDaniel).orElseThrow();
        assertThat(accesoDaniel.getTipoResultado()).isEqualTo(ResultadoAcceso.DENEGADO);
        assertThat(accesoDaniel.getMotivoDenegacion()).isEqualTo(MotivoDenegacion.YA_DENTRO);
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
                primeraCuota.getMes(), primeraCuota.getAnyo(), EstadoCuota.PENDIENTE))
                .hasSize(6)
                .allSatisfy(moroso -> {
                    var cuota = cuotas.findById(moroso.idCuota()).orElseThrow();
                    assertThat(moroso.importe()).isEqualByComparingTo(cuota.getImporte());
                    assertThat(moroso.idSocio()).isEqualTo(cuota.getIdSocio().getId());
                });
        assertThat(cuotas.resumirCuotasPagadasPorMesYPlan(
                primeraCuota.getAnyo(), EstadoCuota.PAGADA)).isNotEmpty();

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
        assertThat(cuotaPagada.getEstado()).isEqualTo(EstadoCuota.PAGADA);

        assertThat(cuotas.ingresosCobradosPorMesYPlan(
                cuotaPagada.getFechaPago().getYear(), EstadoCuota.PAGADA.name())).isNotEmpty();

        LocalDate fecha = accesos.findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc(
                idAna).orElseThrow().getFechaHoraEntrada().toLocalDate();
        LocalDateTime desde = fecha.atStartOfDay();

        assertThat(accesos.contarEntradasPorHora(
                ResultadoAcceso.PERMITIDO.name(), desde, fecha.plusDays(1).atStartOfDay())).isNotEmpty();
    }

    @Test
    void q4_modificacionMasiva() {
        assertThat(ana()).isNotNull(); // Comprueba el esquema de pruebas.
        Socio moroso = socios.findByDniIgnoreCase("00000015S").orElseThrow();
        assertThat(moroso.getActivo()).isTrue();

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid")).withNano(0);
        assertThat(socios.desactivarSociosMorosos(EstadoCuota.PENDIENTE, 2, ahora)).isEqualTo(1);
        Socio desactivado = socios.findById(moroso.getId()).orElseThrow();
        assertThat(desactivado.getActivo()).isFalse();
        assertThat(desactivado.getActualizadoEn()).isEqualTo(ahora);
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
                cuota.getAnyo(), EstadoCuota.PAGADA)).isNotEmpty();
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

    @Test
    void informeDeCobrosAgrupaPorFechaDePagoYConservaCuotasSinPlanHistorico() {
        Socio socio = ana();
        cuotas.saveAndFlush(Cuota.builder().idSocio(socio).mes((short) 12).anyo((short) 2098)
                .importe(new BigDecimal("17.00")).estado(EstadoCuota.PAGADA)
                .fechaPago(LocalDate.of(2099, 1, 2)).build());

        assertThat(cuotas.resumirCuotasPagadasPorMesYPlan((short) 2098, EstadoCuota.PAGADA))
                .singleElement().satisfies(devengo -> {
                    assertThat(devengo.mes()).isEqualTo((short) 12);
                    assertThat(devengo.nombrePlan()).isNull();
                });
        assertThat(cuotas.ingresosCobradosPorMesYPlan(2099, EstadoCuota.PAGADA.name()))
                .singleElement().satisfies(cobro -> {
                    assertThat(cobro[0]).isEqualTo(1);
                    assertThat(cobro[1]).isNull();
                    assertThat((BigDecimal) cobro[2]).isEqualByComparingTo("17.00");
                });
    }

    @Test
    void cambiarPlanConservaCuotasHistoricas() {
        Socio socio = ana();
        var cuota = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                socio.getId(), PageRequest.of(0, 1)).getContent().getFirst();
        Long idPlanHistorico = cuota.getPlan().getId();
        var importeHistorico = cuota.getImporte();
        Long idBasico = planes.findByNombreIgnoreCase("BASICO").orElseThrow().getId();

        var actualizado = socioService.actualizar(socio.getId(), new SocioUpdateRequest(
                socio.getEmail(), socio.getTelefono(), socio.getMatricula(), idBasico));
        entityManager.clear();

        var cuotaHistorica = cuotas.findById(cuota.getId()).orElseThrow();
        assertThat(actualizado.plan().id()).isEqualTo(idBasico).isNotEqualTo(idPlanHistorico);
        assertThat(cuotaHistorica.getPlan().getId()).isEqualTo(idPlanHistorico);
        assertThat(cuotaHistorica.getImporte()).isEqualByComparingTo(importeHistorico);
    }

    @Test
    void reservaCanceladaPermiteReservarOtraVezSinDuplicarLaActiva() {
        Socio socio = ana();
        var cancelada = reservas.findByIdSocio_IdAndEstado(socio.getId(), Estado.CANCELADA).getFirst();
        Long actividadId = cancelada.getIdActividad().getId();
        reservas.saveAndFlush(Reserva.builder().idSocio(socio).idActividad(cancelada.getIdActividad())
                .estado(Estado.ACTIVA).fechaReserva(LocalDateTime.now()).build());

        assertThat(reservas.existsByIdSocio_IdAndIdActividad_IdAndEstado(
                socio.getId(), actividadId, Estado.ACTIVA)).isTrue();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO reserva (id_socio, id_actividad, estado) VALUES (?, ?, 'ACTIVA')
                """, socio.getId(), actividadId)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
