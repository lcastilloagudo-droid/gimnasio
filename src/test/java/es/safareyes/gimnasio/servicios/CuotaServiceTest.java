package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.request.PagoCuotaRequest;
import es.safareyes.gimnasio.dto.request.PeriodoCuotasRequest;
import es.safareyes.gimnasio.dto.response.GeneracionCuotasResponse;
import es.safareyes.gimnasio.exception.ConflictoException;
import es.safareyes.gimnasio.exception.RecursoNoEncontradoException;
import es.safareyes.gimnasio.mapper.CuotaMapperImpl;
import es.safareyes.gimnasio.mapper.PlanMapperImpl;
import es.safareyes.gimnasio.modelos.Cuota;
import es.safareyes.gimnasio.modelos.EstadoCuota;
import es.safareyes.gimnasio.modelos.Plan;
import es.safareyes.gimnasio.modelos.Socio;
import es.safareyes.gimnasio.repositorios.ICuotaRepository;
import es.safareyes.gimnasio.repositorios.ISocioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CuotaServiceTest {
    private final ICuotaRepository cuotas = mock(ICuotaRepository.class);
    private final ISocioRepository socios = mock(ISocioRepository.class);
    private final CuotaService servicio = new CuotaService(cuotas, socios, new CuotaMapperImpl(new PlanMapperImpl()));

    @Test
    void generaSoloCuotasNuevasConPlanEImporteVigentes() {
        short mes = 4;
        short anio = 2090;
        var plan = Plan.builder().id(2L).precioMensual(new BigDecimal("50.00")).build();
        var nuevo = Socio.builder().id(1L).plan(plan).build();
        var conCuota = Socio.builder().id(2L).plan(plan).build();
        when(socios.desactivarSociosMorosos(eq(EstadoCuota.PENDIENTE), eq(2L), eq(mes), eq(anio), any()))
                .thenReturn(1);
        when(socios.findByActivoTrue(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(nuevo, conCuota)));
        when(cuotas.existsByIdSocio_IdAndMesAndAnyo(2L, mes, anio)).thenReturn(true);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cuota>> guardadas = ArgumentCaptor.forClass(List.class);

        assertThat(servicio.generar(new PeriodoCuotasRequest(4, 2090)))
                .isEqualTo(new GeneracionCuotasResponse(1, 1));

        var orden = inOrder(socios, cuotas);
        orden.verify(socios).bloquearParaGenerarCuotas();
        orden.verify(socios).desactivarSociosMorosos(eq(EstadoCuota.PENDIENTE), eq(2L), eq(mes), eq(anio), any());
        orden.verify(socios).findByActivoTrue(Pageable.unpaged());
        orden.verify(cuotas).saveAll(guardadas.capture());
        assertThat(guardadas.getValue()).singleElement().satisfies(cuota -> {
            assertThat(cuota.getIdSocio()).isSameAs(nuevo);
            assertThat(cuota.getPlan()).isSameAs(plan);
            assertThat(cuota.getImporte()).isEqualByComparingTo("50.00");
            assertThat(cuota.getMes()).isEqualTo(mes);
            assertThat(cuota.getAnyo()).isEqualTo(anio);
            assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PENDIENTE);
            assertThat(cuota.getFechaPago()).isNull();
        });
    }

    @Test
    void pagarConservaDatosHistoricosYNoReactivaSocio() {
        var socio = Socio.builder().id(1L).activo(false).build();
        var plan = Plan.builder().id(2L).precioMensual(new BigDecimal("55.00")).build();
        var cuota = Cuota.builder().id(3L).idSocio(socio).plan(plan).estado(EstadoCuota.PENDIENTE)
                .importe(new BigDecimal("50.00")).build();
        when(cuotas.bloquearParaPago(3L)).thenReturn(Optional.of(cuota));
        when(cuotas.save(cuota)).thenReturn(cuota);

        ZoneId madrid = ZoneId.of("Europe/Madrid");
        LocalDate antes = LocalDate.now(madrid);
        var respuesta = servicio.pagar(new PagoCuotaRequest(3L));

        assertThat(respuesta.estado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(respuesta.fechaPago()).isBetween(antes, LocalDate.now(madrid));
        assertThat(respuesta.importe()).isEqualByComparingTo("50.00");
        assertThat(respuesta.plan().id()).isEqualTo(2L);
        assertThat(socio.getActivo()).isFalse();
        verifyNoInteractions(socios);
    }

    @Test
    void pagoRechazaCuotaInexistenteOYaPagada() {
        when(cuotas.bloquearParaPago(1L)).thenReturn(Optional.of(
                Cuota.builder().estado(EstadoCuota.PAGADA).build()));

        assertThatThrownBy(() -> servicio.pagar(new PagoCuotaRequest(9L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.pagar(new PagoCuotaRequest(1L)))
                .isInstanceOf(ConflictoException.class);
        verify(cuotas, never()).save(any());
    }
}
