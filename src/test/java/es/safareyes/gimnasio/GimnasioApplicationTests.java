package es.safareyes.gimnasio;

import es.safareyes.gimnasio.repositorios.IPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class GimnasioApplicationTests {
    @Autowired private MockMvc mvc;
    @Autowired private IPlanRepository planes;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void flujoDeSociosYPlanesContraPostgres() throws Exception {
        Long planId = planes.findByNombreIgnoreCase("PREMIUM").orElseThrow().getId();
        String socios = "/api/v1/socios";

        mvc.perform(get("/api/v1/planes"))
                .andExpectAll(status().isOk(), jsonPath("$[0].tipo").value("BASICO"));

        String ficha = mvc.perform(post(socios).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"12345678z","nombre":"Ana","apellidos":"Ruiz",
                                 "email":"ana.nueva@ejemplo.test","matricula":"1234BCD","idPlan":%d}
                                """.formatted(planId)))
                .andExpectAll(status().isCreated(), header().exists("Location"),
                        jsonPath("$.activo").value(true), jsonPath("$.dni").value("12345678Z"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(ficha))
                .andExpectAll(status().isOk(), jsonPath("$.plan.tipo").value("PREMIUM"));

        mvc.perform(patch(ficha + "/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activo":false}
                                """))
                .andExpectAll(status().isOk(), jsonPath("$.activo").value(false));

        mvc.perform(put(ficha).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana.actualizada@ejemplo.test","telefono":"611123123",
                                 "matricula":null,"idPlan":null}
                                """))
                .andExpectAll(status().isOk(), jsonPath("$.telefono").value("611123123"),
                        jsonPath("$.matricula").doesNotExist(), jsonPath("$.plan").doesNotExist());

        mvc.perform(get(socios).param("activo", "false").param("texto", "12345678Z")
                        .param("size", "1").param("sort", "apellidos,desc"))
                .andExpectAll(status().isOk(), jsonPath("$.totalElements").value(1),
                        jsonPath("$.content[0].email").value("ana.actualizada@ejemplo.test"));

        mvc.perform(get(socios).param("sort", "plan"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.sort").isString());

        mvc.perform(get(socios).param("fechaAltaDesde", "2026-02-01").param("fechaAltaHasta", "2026-01-01"))
                .andExpectAll(status().isBadRequest(), jsonPath("$.errores.fechaAltaDesde").isString());
    }
}
