package es.safareyes.gimnasio.mapper;

import es.safareyes.gimnasio.dto.response.IntegracionResponse;
import es.safareyes.gimnasio.modelos.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapeosDominioTest {

    @Test
    void mapeaCuotasConSocioAnioYPlanHistorico() {
        var plan = Plan.builder().id(1L).nombre("BASICO").tipo(Tipo.BASICO).build();
        var socio = Socio.builder().id(4L).build();
        var cuota = Cuota.builder().idSocio(socio).anyo((short) 2026).plan(plan)
                .importe(new BigDecimal("30.00")).build();

        var respuesta = new CuotaMapperImpl(new PlanMapperImpl()).toResponseList(List.of(cuota)).getFirst();

        assertThat(respuesta.idSocio()).isEqualTo(4L);
        assertThat(respuesta.anio()).isEqualTo((short) 2026);
        assertThat(respuesta.plan().tipo()).isEqualTo(Tipo.BASICO);
        assertThat(respuesta.importe()).isEqualByComparingTo("30.00");
    }

    @Test
    void mapeaResultadoDeAccesoYConservaElResultadoDeIntegracion() {
        var acceso = Acceso.builder().tipoResultado(ResultadoAcceso.PERMITIDO).build();
        var integracion = new IntegracionResponse("PARKCONTROL", IntegracionResponse.Estado.NO_DISPONIBLE,
                "Bonificación no aplicada");

        var respuesta = new AccesoMapperImpl().toResponse(acceso, integracion);

        assertThat(respuesta.resultado()).isEqualTo(ResultadoAcceso.PERMITIDO);
        assertThat(respuesta.integracion()).isEqualTo(integracion);
    }

    @Test
    void mapeaMonitorPlazasYActividadDeReserva() {
        var monitor = Monitor.builder().nombre("Ana").apellidos("López").build();
        var actividad = Actividad.builder().id(5L).idMonitor(monitor).build();
        var reserva = Reserva.builder().idActividad(actividad).estado(Estado.ACTIVA).build();

        var actividadResponse = new ActividadMapperImpl().toResponse(actividad, 3);
        var reservaResponse = new ReservaMapperImpl().toResponseList(List.of(reserva)).getFirst();

        assertThat(actividadResponse.monitor()).isEqualTo("Ana López");
        assertThat(actividadResponse.plazasLibres()).isEqualTo(3);
        assertThat(reservaResponse.idActividad()).isEqualTo(5L);
    }

    @Test
    void mapeaUsuarioConVinculacionOpcional() {
        var usuario = Usuario.builder().nomUsuario("socio04").rol(Rol.SOCIO)
                .idSocio(Socio.builder().id(4L).build()).build();
        var mapper = new UsuarioMapperImpl();

        assertThat(mapper.toResponse(usuario).nomUsuario()).isEqualTo("socio04");
        assertThat(mapper.toResponse(usuario).idSocio()).isEqualTo(4L);
        assertThat(mapper.toResponse(Usuario.builder().rol(Rol.ADMIN).build()).idSocio()).isNull();
    }
}
