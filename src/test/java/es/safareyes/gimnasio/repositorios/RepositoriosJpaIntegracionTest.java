package es.safareyes.gimnasio.repositorios;

import es.safareyes.gimnasio.dto.response.IngresoMensualDto;
import es.safareyes.gimnasio.modelos.Acceso;
import es.safareyes.gimnasio.modelos.Actividad;
import es.safareyes.gimnasio.modelos.Cuota;
import es.safareyes.gimnasio.modelos.Estado;
import es.safareyes.gimnasio.modelos.Socio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integracion sobre PostgreSQL usando los datos de sql/data.sql.
 * Requieren DB_URL, DB_USER, DB_PASSWORD y DB_SCHEMA=gimnasio_test.
 * @DataJpaTest revierte automaticamente la transaccion de cada prueba.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Repositorios JPA de FitZone con datos de prueba")
class RepositoriosJpaIntegracionTest {

    @Autowired private ISocioRepository socios;
    @Autowired private IPlanRepository planes;
    @Autowired private ICuotaRepository cuotas;
    @Autowired private IAccesoRepository accesos;
    @Autowired private IActividadRepository actividades;
    @Autowired private IReservaRepository reservas;
    @Autowired private IMonitorRepository monitores;
    @Autowired private IUsuarioRepository usuarios;

    @Value("${spring.datasource.hikari.schema}")
    private String esquemaConfigurado;

    @BeforeEach
    void comprobarEsquemaYDatosCargados() {
        assertThat(esquemaConfigurado)
                .as("Las pruebas solo deben ejecutarse sobre DB_SCHEMA=gimnasio_test")
                .isEqualTo("gimnasio_test");
        assertThat(socios.count())
                .as("Carga una sola vez sql/data.sql en gimnasio_test antes de ejecutar los tests")
                .isEqualTo(20);
    }

    private Socio socioNumeroUno() {
        return socios.findByDniIgnoreCase("00000001r").orElseThrow();
    }

    private Actividad claseCompleta() {
        return actividades.findAll().stream()
                .filter(a -> a.getNombre().equals("Spinning") && a.getAforoMax() == 2)
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Q1: consultas derivadas de socio y comprobacion de duplicados")
    void buscarSocioPorDniYEmail() {
        Socio ana = socioNumeroUno();

        assertThat(ana.getNombre()).isEqualTo("Ana");
        assertThat(ana.getPlan().getNombre()).isEqualTo("PREMIUM");
        assertThat(socios.existsByDniIgnoreCase("00000001R")).isTrue();
        assertThat(socios.existsByEmailIgnoreCase("SOCIO01@EJEMPLO.TEST")).isTrue();
        assertThat(socios.countByActivoTrue()).isEqualTo(17);
    }

    @Test
    @DisplayName("Q2: cargar un socio y su plan con JOIN FETCH")
    void buscarSocioConPlan() {
        Socio ana = socioNumeroUno();
        Socio resultado = socios.buscarConPlan(ana.getId()).orElseThrow();

        assertThat(resultado.getPlan()).isNotNull();
        assertThat(resultado.getPlan().getPrecioMensual()).isPositive();
    }

    @Test
    @DisplayName("Q5/Q6: proyeccion de socios y paginacion")
    void proyeccionYPaginacion() {
        Page<ISocioRepository.SocioResumen> resumen =
                socios.findByNombreContainingIgnoreCase("ana", PageRequest.of(0, 5));
        assertThat(resumen.getContent()).extracting(ISocioRepository.SocioResumen::getNombre)
                .contains("Ana");

        Page<Socio> pagina = socios.findByActivo(
                true, PageRequest.of(0, 5, Sort.by("nombre").ascending()));
        assertThat(pagina.getTotalElements()).isEqualTo(17);
        assertThat(pagina.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("Q7: combinar estado, plan, texto y fechas con Specification")
    void filtrosDinamicosDeSocios() {
        Socio ana = socioNumeroUno();

        var filtro = SocioSpecifications.conFiltros(
                true, ana.getPlan().getId(), "Lopez",
                ana.getFechaAlta().minusDays(1),
                ana.getFechaAlta().plusDays(1));

        var encontrados = socios.findAll(filtro, PageRequest.of(0, 10));

        assertThat(encontrados.getContent())
                .extracting(Socio::getId).contains(ana.getId());
        assertThat(encontrados.getContent())
                .allSatisfy(s -> {
                    assertThat(s.getActivo()).isTrue();
                    assertThat(s.getPlan().getId()).isEqualTo(ana.getPlan().getId());
                });
    }

    @Test
    @DisplayName("Q4: desactivar un moroso mediante UPDATE sin guardar el cambio")
    void desactivarSociosMorososConRollback() {
        Socio moroso = socios.findByDniIgnoreCase("00000015S").orElseThrow();
        assertThat(moroso.getActivo()).isTrue();

        int modificados = socios.desactivarSociosMorosos("PENDIENTE", 2);

        assertThat(modificados).isEqualTo(1);
        assertThat(socios.findById(moroso.getId()).orElseThrow().getActivo()).isFalse();
        // @DataJpaTest ejecuta ROLLBACK despues de la prueba.
    }

    @Test
    @DisplayName("Cuotas: duplicados e historial paginado")
    void cuotasPorSocioYPeriodo() {
        Long idSocio = socioNumeroUno().getId();
        Page<Cuota> pagina = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                idSocio, PageRequest.of(0, 2));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent()).hasSize(2);

        Cuota reciente = pagina.getContent().get(0);
        assertThat(cuotas.existsByIdSocio_IdAndMesAndAnyo(
                idSocio, reciente.getMes(), reciente.getAnyo())).isTrue();
    }

    @Test
    @DisplayName("Q2: socios con cuotas pendientes de un mes")
    void consultarMorososPorMes() {
        Long socioId = socioNumeroUno().getId();
        Cuota masReciente = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                socioId, PageRequest.of(0, 1)).getContent().get(0);

        List<Socio> morosos = cuotas.buscarSociosConCuotaPendiente(
                masReciente.getMes(), masReciente.getAnyo(), "PENDIENTE");

        assertThat(morosos).hasSize(6);
        assertThat(morosos).extracting(Socio::getDni).contains("00000015S");
    }

    @Test
    @DisplayName("Q2/Q3/Q5: informes de cuotas pagadas JPQL y SQL nativo")
    void informesDeIngresos() {
        Cuota pagada = cuotas.findByIdSocio_IdOrderByAnyoDescMesDesc(
                socioNumeroUno().getId(), PageRequest.of(0, 1)).getContent().get(0);

        List<IngresoMensualDto> devengados =
                cuotas.resumirCuotasPagadasPorMesYPlan(pagada.getAnyo(), "PAGADA");
        assertThat(devengados).isNotEmpty();
        assertThat(devengados).allSatisfy(i -> assertThat(i.total()).isPositive());

        List<Object[]> cobrados =
                cuotas.ingresosCobradosPorMesYPlan(pagada.getFechaPago().getYear(), "PAGADA");
        assertThat(cobrados).isNotEmpty();
    }

    @Test
    @DisplayName("Accesos: entrada abierta, aforo diario y DATE_TRUNC")
    void aforoYHorasPunta() {
        Long idSocio = socioNumeroUno().getId();
        Acceso abierto = accesos
                .findFirstByIdSocio_IdAndTipoResultadoAndFechaHoraSalidaIsNullOrderByFechaHoraEntradaDesc(
                        idSocio, "PERMITIDO")
                .orElseThrow();

        assertThat(accesos.existsByIdSocio_IdAndTipoResultadoAndFechaHoraSalidaIsNull(
                idSocio, "PERMITIDO")).isTrue();

        LocalDate dia = abierto.getFechaHoraEntrada().toLocalDate();
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();

        assertThat(accesos.contarPersonasDentro("PERMITIDO", desde, hasta)).isEqualTo(3);
        assertThat(accesos.contarEntradasPorHora("PERMITIDO", desde, hasta)).isNotEmpty();
    }

    @Test
    @DisplayName("Q8: actividad con monitor y coleccion de reservas precargada")
    void actividadesConReservasSinDuplicados() {
        Actividad especial = claseCompleta();
        LocalDate dia = especial.getFechaHora().toLocalDate();

        List<Actividad> resultados = actividades
                .findDistinctByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                        dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());

        assertThat(resultados).extracting(Actividad::getId).doesNotHaveDuplicates();
        Actividad spinning = resultados.stream()
                .filter(a -> a.getId().equals(especial.getId()))
                .findFirst().orElseThrow();

        assertThat(spinning.getIdMonitor()).isNotNull();
        assertThat(spinning.getReservas())
                .filteredOn(r -> r.getEstado() == Estado.ACTIVA)
                .hasSize(2);

        assertThat(actividades.contarReservasPorActividad(
                dia.atStartOfDay(), dia.plusDays(1).atStartOfDay(), Estado.ACTIVA))
                .isNotEmpty();
    }

    @Test
    @DisplayName("Reservas: no duplicar y maximo de tres futuras")
    void plazasOcupadasYReservasDeSocio() {
        Actividad spinning = claseCompleta();
        Long idSocio = socioNumeroUno().getId();

        assertThat(reservas.countByIdActividad_IdAndEstado(
                spinning.getId(), Estado.ACTIVA)).isEqualTo(2);

        assertThat(reservas.existsByIdSocio_IdAndIdActividad_IdAndEstado(
                idSocio, spinning.getId(), Estado.ACTIVA)).isTrue();

        // Las tres reservas futuras del socio 1 se crearon sobre fechas posteriores
        // al dia de carga. Se usa su primera actividad para no depender de hoy.
        LocalDateTime inicio = reservas.findByIdSocio_IdAndEstado(
                idSocio, Estado.ACTIVA).stream()
                .map(r -> r.getIdActividad().getFechaHora())
                .min(LocalDateTime::compareTo).orElseThrow()
                .minusSeconds(1);

        assertThat(reservas.countByIdSocio_IdAndEstadoAndIdActividad_FechaHoraAfter(
                idSocio, Estado.ACTIVA, inicio)).isEqualTo(3);
    }

    @Test
    @DisplayName("Planes, monitores y usuarios: consultas derivadas")
    void consultarCatalogosYUsuarios() {
        assertThat(planes.count()).isEqualTo(2);
        assertThat(planes.findByNombreIgnoreCase("premium")).isPresent();
        assertThat(planes.findAllByOrderByPrecioMensualAsc())
                .extracting(p -> p.getNombre()).containsExactly("BASICO", "PREMIUM");

        assertThat(monitores.count()).isEqualTo(4);
        assertThat(monitores.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(
                "bea", "valor-imposible")).hasSize(1);

        assertThat(usuarios.count()).isEqualTo(12);
        assertThat(usuarios.findByNomUsuarioIgnoreCase("ADMIN_FITZONE")).isPresent();
        assertThat(usuarios.findByIdSocio_Id(socioNumeroUno().getId())).isPresent();
    }
}
