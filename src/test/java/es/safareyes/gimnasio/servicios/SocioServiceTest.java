package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.request.SocioEstadoRequest;
import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.dto.request.SocioUpdateRequest;
import es.safareyes.gimnasio.exception.ConflictoException;
import es.safareyes.gimnasio.exception.RecursoNoEncontradoException;
import es.safareyes.gimnasio.mapper.PlanMapperImpl;
import es.safareyes.gimnasio.mapper.SocioMapperImpl;
import es.safareyes.gimnasio.modelos.Plan;
import es.safareyes.gimnasio.modelos.Socio;
import es.safareyes.gimnasio.modelos.Tipo;
import es.safareyes.gimnasio.repositorios.IPlanRepository;
import es.safareyes.gimnasio.repositorios.ISocioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SocioServiceTest {
    private ISocioRepository socios;
    private IPlanRepository planes;
    private SocioService servicio;
    private Plan premium;

    @BeforeEach
    void preparar() {
        socios = mock(ISocioRepository.class);
        planes = mock(IPlanRepository.class);
        servicio = new SocioService(socios, planes, new SocioMapperImpl(new PlanMapperImpl()));
        premium = Plan.builder().id(2L).nombre("PREMIUM").tipo(Tipo.PREMIUM)
                .precioMensual(new BigDecimal("50.00")).build();
    }

    private SocioRequest alta() {
        return new SocioRequest("12345678z", "Ana", "Lopez", "ANA@ejemplo.test",
                "600123123", "1234BCD", 2L);
    }

    @Test
    void altaConPlanYDatosIniciales() {
        when(planes.findById(2L)).thenReturn(Optional.of(premium));
        when(socios.saveAndFlush(any(Socio.class))).thenAnswer(invocacion -> {
            Socio socio = invocacion.getArgument(0);
            socio.setId(10L);
            return socio;
        });

        var respuesta = servicio.createSocio(alta());

        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.dni()).isEqualTo("12345678Z");
        assertThat(respuesta.email()).isEqualTo("ana@ejemplo.test");
        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.fechaAlta()).isNotNull();
        assertThat(respuesta.plan().tipo()).isEqualTo(Tipo.PREMIUM);
    }

    @Test
    void rechazaDniDuplicado() {
        when(socios.existsByDniIgnoreCase("12345678Z")).thenReturn(true);
        assertThatThrownBy(() -> servicio.createSocio(alta())).isInstanceOf(ConflictoException.class);
        verify(socios, never()).saveAndFlush(any(Socio.class));
    }

    @Test
    void rechazaEmailDuplicado() {
        when(socios.existsByEmailIgnoreCase("ana@ejemplo.test")).thenReturn(true);
        assertThatThrownBy(() -> servicio.createSocio(alta())).isInstanceOf(ConflictoException.class);
        verify(socios, never()).saveAndFlush(any(Socio.class));
    }

    @Test
    void socioInexistente() {
        when(socios.buscarConPlan(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.getSocioById(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarPermiteEliminarMatriculaYNoCambiaIdentidad() {
        Socio socio = Socio.builder().id(10L).dni("12345678Z").nombre("Ana")
                .email("ana@ejemplo.test").matricula("1234BCD").activo(true).build();
        when(socios.buscarConPlan(10L)).thenReturn(Optional.of(socio));
        when(planes.findById(2L)).thenReturn(Optional.of(premium));
        when(socios.saveAndFlush(socio)).thenReturn(socio);

        var respuesta = servicio.actualizar(10L, new SocioUpdateRequest(
                "ana@ejemplo.test", null, null, 2L));

        assertThat(respuesta.matricula()).isNull();
        assertThat(respuesta.dni()).isEqualTo("12345678Z");
        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.plan().id()).isEqualTo(2L);
    }

    @Test
    void cambioDeEstadoNoModificaContactoNiPlan() {
        Socio socio = Socio.builder().id(10L).email("ana@ejemplo.test")
                .plan(premium).activo(true).build();
        when(socios.buscarConPlan(10L)).thenReturn(Optional.of(socio));
        when(socios.saveAndFlush(socio)).thenReturn(socio);

        var respuesta = servicio.cambiarEstado(10L, new SocioEstadoRequest(false));

        assertThat(respuesta.activo()).isFalse();
        assertThat(respuesta.email()).isEqualTo("ana@ejemplo.test");
        assertThat(respuesta.plan().id()).isEqualTo(2L);
    }
}
