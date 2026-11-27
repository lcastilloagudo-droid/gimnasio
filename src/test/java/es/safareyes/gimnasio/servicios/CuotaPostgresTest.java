package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.request.PagoCuotaRequest;
import es.safareyes.gimnasio.dto.request.PeriodoCuotasRequest;
import es.safareyes.gimnasio.dto.request.SocioUpdateRequest;
import es.safareyes.gimnasio.dto.response.GeneracionCuotasResponse;
import es.safareyes.gimnasio.exception.ConflictoException;
import es.safareyes.gimnasio.modelos.EstadoCuota;
import es.safareyes.gimnasio.repositorios.ICuotaRepository;
import es.safareyes.gimnasio.repositorios.IPlanRepository;
import es.safareyes.gimnasio.repositorios.ISocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CuotaPostgresTest {
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final String CUOTAS = "/api/v1/cuotas";

    @Autowired private CuotaService servicio;
    @Autowired private SocioService socioService;
    @Autowired private ICuotaRepository cuotas;
    @Autowired private ISocioRepository socios;
    @Autowired private IPlanRepository planes;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private MockMvc mvc;

    @Test
    @Transactional
    void generacionDesactivaMorososYUnReintentoNoCuentaLaCuotaRecienCreada() throws Exception {
        var periodo = new PeriodoCuotasRequest(4, 2090);

        mvc.perform(post(CUOTAS + "/generar").param("mes", "4").param("anio", "2090"))
                .andExpectAll(status().isOk(), jsonPath("$.cuotasCreadas").value(16),
                        jsonPath("$.sociosDesactivados").value(1));

        var moroso = socios.findByDniIgnoreCase("00000015S").orElseThrow();
        var unaPendiente = socios.findByDniIgnoreCase("00000002W").orElseThrow();
        assertThat(moroso.getActivo()).isFalse();
        assertThat(moroso.getActualizadoEn().toLocalDate()).isEqualTo(LocalDate.now(MADRID));
        assertThat(cuotas.existsByIdSocio_IdAndMesAndAnyo(moroso.getId(), (short) 4, (short) 2090)).isFalse();
        assertThat(unaPendiente.getActivo()).isTrue();
        assertThat(servicio.generar(periodo)).isEqualTo(new GeneracionCuotasResponse(0, 0));
        assertThat(socios.findById(unaPendiente.getId()).orElseThrow().getActivo()).isTrue();

        var pendientes = servicio.pendientes(periodo);
        assertThat(pendientes).hasSize(16);
        assertThat(pendientes.stream().map(p -> p.idSocio()).toList()).doesNotHaveDuplicates();
        assertThat(pendientes).allSatisfy(pendiente ->
                assertThat(socios.findById(pendiente.idSocio()).orElseThrow().getActivo()).isTrue());
    }

    @Test
    @Transactional
    void otrosMesesYAniosConservanLasCuotasHistoricas() {
        var ana = socios.findByDniIgnoreCase("00000001R").orElseThrow();
        Long idPremium = ana.getPlan().getId();
        var precioPremium = ana.getPlan().getPrecioMensual();
        servicio.generar(new PeriodoCuotasRequest(4, 2090));
        var historica = servicio.historial(ana.getId(), PageRequest.of(0, 1)).getContent().getFirst();
        servicio.pagar(new PagoCuotaRequest(historica.id()));

        var basico = planes.findByNombreIgnoreCase("BASICO").orElseThrow();
        socioService.actualizar(ana.getId(), new SocioUpdateRequest(ana.getEmail(), null, null, basico.getId()));
        assertThat(servicio.generar(new PeriodoCuotasRequest(5, 2090)).cuotasCreadas()).isPositive();
        assertThat(servicio.generar(new PeriodoCuotasRequest(1, 2091)).cuotasCreadas()).isPositive();

        var historial = servicio.historial(ana.getId(), PageRequest.of(0, 3)).getContent();
        assertThat(historial).extracting(c -> c.anio() + "-" + c.mes()).containsExactly("2091-1", "2090-5", "2090-4");
        assertThat(historial.getFirst().plan().id()).isEqualTo(basico.getId());
        assertThat(historial.getFirst().importe()).isEqualByComparingTo(basico.getPrecioMensual());
        assertThat(historial.getLast().plan().id()).isEqualTo(idPremium);
        assertThat(historial.getLast().importe()).isEqualByComparingTo(precioPremium);
        assertThat(historial.getLast().estado()).isEqualTo(EstadoCuota.PAGADA);
    }

    @Test
    @Transactional
    void pagoEHistorialYPendientesSeSirvenPorHttp() throws Exception {
        var periodo = new PeriodoCuotasRequest(6, 2090);
        servicio.generar(periodo);
        var pendiente = servicio.pendientes(periodo).getFirst();
        long idCuota = pendiente.idCuota();
        long idSocio = pendiente.idSocio();
        String historial = "/api/v1/socios/" + idSocio + "/cuotas";

        mvc.perform(get(CUOTAS + "/pendientes").param("mes", "6").param("anio", "2090"))
                .andExpectAll(status().isOk(), jsonPath("$", hasSize(16)),
                        jsonPath("$[0].idCuota").value(idCuota), jsonPath("$[0].importe").isNumber());
        mvc.perform(get(historial).param("page", "0").param("size", "1"))
                .andExpectAll(status().isOk(), jsonPath("$.content[0].id").value(idCuota),
                        jsonPath("$.content[0].plan.id").isNumber(), jsonPath("$.totalElements").value(4));
        mvc.perform(get(historial).param("page", "1").param("size", "1"))
                .andExpectAll(status().isOk(), jsonPath("$.content[0].anio").value(LocalDate.now(MADRID).getYear()));
        mvc.perform(get(historial).param("size", "1").param("sort", "anio,asc"))
                .andExpectAll(status().isOk(), jsonPath("$.content[0].id").isNumber());

        String pago = """
                {"idCuota":%d}
                """.formatted(idCuota);
        mvc.perform(post(CUOTAS + "/pagar").contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpectAll(status().isOk(), jsonPath("$.estado").value("PAGADA"),
                        jsonPath("$.fechaPago").value(LocalDate.now(MADRID).toString()),
                        jsonPath("$.importe").value(pendiente.importe().doubleValue()));
        assertThat(cuotas.existsByIdSocio_IdAndMesAndAnyoAndEstado(
                idSocio, (short) 6, (short) 2090, EstadoCuota.PAGADA)).isTrue();
        mvc.perform(post(CUOTAS + "/pagar").contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpectAll(status().isConflict(), jsonPath("$.status").value(409));
        mvc.perform(get(CUOTAS + "/pendientes").param("mes", "6").param("anio", "2090"))
                .andExpectAll(status().isOk(), jsonPath("$", hasSize(15)));
        mvc.perform(get(CUOTAS + "/pendientes").param("mes", "7").param("anio", "2090"))
                .andExpectAll(status().isOk(), jsonPath("$", hasSize(0)));
        mvc.perform(get(CUOTAS + "/pendientes").param("mes", "6").param("anio", "2091"))
                .andExpectAll(status().isOk(), jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void historialVacioYValidacionesHttp() throws Exception {
        var nuevo = socios.findByDniIgnoreCase("12345678Z");
        assertThat(nuevo).isEmpty();
        String ficha = mvc.perform(post("/api/v1/socios").contentType(MediaType.APPLICATION_JSON).content("""
                {"dni":"12345678Z","nombre":"Ana","apellidos":"López","email":"sin-cuotas@ejemplo.test"}
                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

        mvc.perform(get(ficha + "/cuotas"))
                .andExpectAll(status().isOk(), jsonPath("$.content", hasSize(0)));
        mvc.perform(get(ficha + "/cuotas").param("sort", "plan"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.sort").isString());
        mvc.perform(get("/api/v1/socios/9223372036854775807/cuotas"))
                .andExpectAll(status().isNotFound(), jsonPath("$.status").value(404));
        mvc.perform(get("/api/v1/socios/0/cuotas"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.id").isString());
        mvc.perform(post(CUOTAS + "/pagar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCuota\":9223372036854775807}"))
                .andExpectAll(status().isNotFound(), jsonPath("$.status").value(404));
        mvc.perform(post(CUOTAS + "/pagar").contentType(MediaType.APPLICATION_JSON).content("{\"idCuota\":0}"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.idCuota").isString());
        mvc.perform(post(CUOTAS + "/generar").param("mes", "0").param("anio", "2101"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.mes").isString(),
                        jsonPath("$.errores.anio").isString());
        mvc.perform(get(CUOTAS + "/pendientes").param("mes", "abril").param("anio", "2090"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.mes").isString());
        mvc.perform(get(CUOTAS + "/pendientes").param("mes", "4"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.anio").isString());
    }

    @Test
    void dosGeneracionesConcurrentesCreanUnSoloLote() throws Exception {
        var periodo = new PeriodoCuotasRequest(8, 2090);
        var moroso = socios.findByDniIgnoreCase("00000015S").orElseThrow();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cuota WHERE mes = ? AND anio = ?",
                Long.class, periodo.mes(), periodo.anio())).isZero();
        assertThat(moroso.getActivo()).isTrue();

        try {
            ejecutarSolapadas(() -> servicio.generar(periodo), () -> servicio.generar(periodo),
                    new GeneracionCuotasResponse(16, 1), new GeneracionCuotasResponse(0, 0), null);
            assertThat(servicio.pendientes(periodo)).hasSize(16);
        } finally {
            // Solo se retira el lote creado por esta prueba en la base dedicada.
            jdbc.update("DELETE FROM cuota WHERE mes = ? AND anio = ?", periodo.mes(), periodo.anio());
            jdbc.update("UPDATE socio SET activo = true, actualizado_en = ? WHERE id_socio = ?",
                    moroso.getActualizadoEn(), moroso.getId());
        }
    }

    @Test
    void laGeneracionEsperaElCambioDePlanYUsaElPrecioConfirmado() throws Exception {
        var ana = socios.findByDniIgnoreCase("00000001R").orElseThrow();
        var planAnterior = planes.findByNombreIgnoreCase("PREMIUM").orElseThrow();
        var basico = planes.findByNombreIgnoreCase("BASICO").orElseThrow();
        var periodo = new PeriodoCuotasRequest(9, 2090);

        try {
            ejecutarSolapadas(
                    () -> socioService.actualizar(ana.getId(), new SocioUpdateRequest(
                            ana.getEmail(), ana.getTelefono(), ana.getMatricula(), basico.getId())).plan().id(),
                    () -> new TransactionTemplate(transacciones).execute(estado -> {
                        servicio.generar(periodo);
                        var cuota = servicio.historial(ana.getId(), PageRequest.of(0, 1)).getContent().getFirst();
                        assertThat(cuota.importe()).isEqualByComparingTo(basico.getPrecioMensual());
                        estado.setRollbackOnly();
                        return cuota.plan().id();
                    }),
                    basico.getId(), basico.getId(), null);
        } finally {
            jdbc.update("UPDATE socio SET id_plan = ?, actualizado_en = ? WHERE id_socio = ?",
                    planAnterior.getId(), ana.getActualizadoEn(), ana.getId());
        }
    }

    @Test
    void dosPagosConcurrentesDanUnSoloExito() throws Exception {
        var socio = socios.findByDniIgnoreCase("00000007F").orElseThrow();
        var anterior = servicio.historial(socio.getId(), PageRequest.of(0, 1)).getContent().getFirst();
        assertThat(anterior.estado()).isEqualTo(EstadoCuota.PENDIENTE);
        var request = new PagoCuotaRequest(anterior.id());

        try {
            ejecutarSolapadas(() -> servicio.pagar(request).estado(), () -> servicio.pagar(request).estado(),
                    EstadoCuota.PAGADA, null, ConflictoException.class);
            var pagada = cuotas.findById(anterior.id()).orElseThrow();
            assertThat(pagada.getEstado()).isEqualTo(EstadoCuota.PAGADA);
            assertThat(pagada.getFechaPago()).isEqualTo(LocalDate.now(MADRID));
            assertThat(pagada.getImporte()).isEqualByComparingTo(anterior.importe());
            assertThat(socios.findById(socio.getId()).orElseThrow().getActivo()).isFalse();
        } finally {
            jdbc.update("UPDATE cuota SET estado = 'PENDIENTE', fecha_pago = NULL WHERE id_cuota = ?", anterior.id());
        }
    }

    private <T> void ejecutarSolapadas(
            Callable<T> primera, Callable<T> segunda, T resultadoPrimero, T resultadoSegundo,
            Class<? extends Throwable> errorSegundo) throws Exception {
        var primeraEscribio = new CountDownLatch(1);
        var segundaEmpezo = new CountDownLatch(1);
        var permitirCommit = new CountDownLatch(1);
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var primerResultado = hilos.submit(() -> new TransactionTemplate(transacciones).execute(estado -> {
                try {
                    T resultado = primera.call();
                    primeraEscribio.countDown();
                    assertThat(permitirCommit.await(10, TimeUnit.SECONDS)).isTrue();
                    return resultado;
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }));

            try {
                assertThat(primeraEscribio.await(10, TimeUnit.SECONDS)).isTrue();
                var segundoResultado = hilos.submit(() -> {
                    segundaEmpezo.countDown();
                    return segunda.call();
                });
                assertThat(segundaEmpezo.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> segundoResultado.get(250, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);

                permitirCommit.countDown();
                assertThat(primerResultado.get(10, TimeUnit.SECONDS)).isEqualTo(resultadoPrimero);
                if (errorSegundo == null) {
                    assertThat(segundoResultado.get(10, TimeUnit.SECONDS)).isEqualTo(resultadoSegundo);
                } else {
                    assertThatThrownBy(() -> segundoResultado.get(10, TimeUnit.SECONDS))
                            .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(errorSegundo);
                }
            } finally {
                permitirCommit.countDown();
            }
        }
    }
}
