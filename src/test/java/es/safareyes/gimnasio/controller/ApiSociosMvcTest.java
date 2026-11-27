package es.safareyes.gimnasio.controller;

import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.dto.response.PlanResponse;
import es.safareyes.gimnasio.dto.response.SocioResponse;
import es.safareyes.gimnasio.exception.ConflictoException;
import es.safareyes.gimnasio.exception.IntegracionException;
import es.safareyes.gimnasio.exception.RecursoNoEncontradoException;
import es.safareyes.gimnasio.exception.ReglaNegocioException;
import es.safareyes.gimnasio.modelos.Tipo;
import es.safareyes.gimnasio.servicios.PlanService;
import es.safareyes.gimnasio.servicios.SocioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({SocioController.class, PlanController.class})
class ApiSociosMvcTest {
    private static final String SOCIOS = "/api/v1/socios";
    private static final String ALTA = """
            {"dni":"12345678Z","nombre":"Ana","apellidos":"López","email":"ana@ejemplo.test","idPlan":2}
            """;

    @Autowired private MockMvc mvc;
    @MockitoBean private SocioService socios;
    @MockitoBean private PlanService planes;

    @Test
    void altaDevuelveCreatedLocationYPlan() throws Exception {
        var plan = new PlanResponse(2L, "PREMIUM", Tipo.PREMIUM, new BigDecimal("50.00"));
        when(socios.createSocio(any(SocioRequest.class))).thenReturn(new SocioResponse(
                10L, "12345678Z", "Ana", "López", "ana@ejemplo.test", null,
                LocalDate.of(2026, 11, 27), true, null, plan));

        mvc.perform(post(SOCIOS).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", SOCIOS + "/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.plan.tipo").value("PREMIUM"));
    }

    @Test
    void cuerpoYParametrosInvalidosCompartenFormato() throws Exception {
        mvc.perform(post(SOCIOS).contentType(MediaType.APPLICATION_JSON)
                        .content(ALTA.replace("12345678Z", "12345678A")))
                .andExpectAll(status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                        jsonPath("$.errores.dni").isString(), jsonPath("$.timestamp").exists());

        mvc.perform(get(SOCIOS).param("idPlan", "0"))
                .andExpectAll(status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                        jsonPath("$.errores.idPlan").isString(), jsonPath("$.timestamp").exists());

        mvc.perform(get(SOCIOS).param("fechaAltaDesde", "ayer"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.fechaAltaDesde").isString());

        mvc.perform(put(SOCIOS + "/10").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.email").isString());
    }

    @Test
    void reglasYRecursosDevuelvenSuCodigo() throws Exception {
        when(socios.getSocioById(10L)).thenThrow(new RecursoNoEncontradoException("Socio inexistente"));
        mvc.perform(get(SOCIOS + "/10"))
                .andExpectAll(status().isNotFound(), jsonPath("$.instance").value(SOCIOS + "/10"));

        when(socios.createSocio(any(SocioRequest.class))).thenThrow(new ConflictoException("DNI duplicado"));
        mvc.perform(post(SOCIOS).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpectAll(status().isConflict(), jsonPath("$.detail").value("DNI duplicado"));

        when(socios.cambiarEstado(any(), any())).thenThrow(new ReglaNegocioException("Falta un plan"));
        mvc.perform(patch(SOCIOS + "/10/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activo":true}
                                """))
                .andExpectAll(status().is(422), jsonPath("$.detail").value("Falta un plan"));

        when(planes.listar()).thenThrow(new IntegracionException("Detalle interno"));
        mvc.perform(get("/api/v1/planes"))
                .andExpectAll(status().isServiceUnavailable(), content().string(not(containsString("Detalle interno"))));
    }

    @Test
    void distingueUnicidadYChecksSinExponerDetallesSql() throws Exception {
        when(socios.createSocio(any(SocioRequest.class))).thenThrow(
                new DataIntegrityViolationException("detalle interno", new SQLException("restricción interna", "23505")),
                new DataIntegrityViolationException("detalle interno", new SQLException("restricción interna", "23514")));
        mvc.perform(post(SOCIOS).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpectAll(status().isConflict(), content().string(not(containsString("interna"))));
        mvc.perform(post(SOCIOS).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpectAll(status().is(422), content().string(not(containsString("interna"))));
    }
}
