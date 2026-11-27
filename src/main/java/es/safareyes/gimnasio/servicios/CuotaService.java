package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.request.PagoCuotaRequest;
import es.safareyes.gimnasio.dto.request.PeriodoCuotasRequest;
import es.safareyes.gimnasio.dto.response.CuotaResponse;
import es.safareyes.gimnasio.dto.response.GeneracionCuotasResponse;
import es.safareyes.gimnasio.dto.response.SocioMorosoResponse;
import es.safareyes.gimnasio.exception.ConflictoException;
import es.safareyes.gimnasio.exception.PeticionInvalidaException;
import es.safareyes.gimnasio.exception.RecursoNoEncontradoException;
import es.safareyes.gimnasio.mapper.CuotaMapper;
import es.safareyes.gimnasio.modelos.Cuota;
import es.safareyes.gimnasio.modelos.EstadoCuota;
import es.safareyes.gimnasio.modelos.Plan;
import es.safareyes.gimnasio.modelos.Socio;
import es.safareyes.gimnasio.repositorios.ICuotaRepository;
import es.safareyes.gimnasio.repositorios.ISocioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CuotaService {
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final int MINIMO_PENDIENTES = 2;
    private static final Set<String> CAMPOS_ORDEN = Set.of("id", "mes", "anio", "importe", "estado", "fechaPago");

    private final ICuotaRepository cuotaRepository;
    private final ISocioRepository socioRepository;
    private final CuotaMapper cuotaMapper;

    @Transactional
    public GeneracionCuotasResponse generar(PeriodoCuotasRequest periodo) {
        short mes = periodo.mes().shortValue();
        short anio = periodo.anio().shortValue();
        socioRepository.bloquearParaGenerarCuotas();

        // Una cuota del periodo solicitado no cuenta como deuda previa en sus reintentos.
        int desactivados = socioRepository.desactivarSociosMorosos(
                EstadoCuota.PENDIENTE, MINIMO_PENDIENTES, mes, anio, LocalDateTime.now(MADRID));

        List<Cuota> nuevas = new ArrayList<>();
        for (Socio socio : socioRepository.findByActivoTrue(Pageable.unpaged())) {
            Plan plan = socio.getPlan();
            if (plan == null || cuotaRepository.existsByIdSocio_IdAndMesAndAnyo(socio.getId(), mes, anio)) {
                continue;
            }
            nuevas.add(Cuota.builder()
                    .idSocio(socio)
                    .plan(plan)
                    .importe(plan.getPrecioMensual())
                    .mes(mes)
                    .anyo(anio)
                    .estado(EstadoCuota.PENDIENTE)
                    .build());
        }

        cuotaRepository.saveAll(nuevas);
        log.info("Generación de cuotas {}/{}: creadas={}, socios desactivados={}",
                mes, anio, nuevas.size(), desactivados);
        return new GeneracionCuotasResponse(nuevas.size(), desactivados);
    }

    @Transactional
    public CuotaResponse pagar(PagoCuotaRequest request) {
        Cuota cuota = cuotaRepository.bloquearParaPago(request.idCuota())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuota con id " + request.idCuota()));
        if (cuota.getEstado() == EstadoCuota.PAGADA) {
            throw new ConflictoException("La cuota ya está pagada");
        }

        cuota.setEstado(EstadoCuota.PAGADA);
        cuota.setFechaPago(LocalDate.now(MADRID));
        log.info("Pago registrado para la cuota {}", cuota.getId());
        return cuotaMapper.toResponse(cuotaRepository.save(cuota));
    }

    public Page<CuotaResponse> historial(Long idSocio, Pageable pageable) {
        if (!socioRepository.existsById(idSocio)) {
            throw new RecursoNoEncontradoException("No existe el socio con id " + idSocio);
        }
        if (pageable.getSort().isUnsorted()) {
            // El periodo es único por socio, así que el orden se mantiene entre páginas.
            return cuotaRepository.findByIdSocio_IdOrderByAnyoDescMesDesc(idSocio, pageable)
                    .map(cuotaMapper::toResponse);
        }

        Sort orden = Sort.by(pageable.getSort().stream().map(campo -> {
            if (!CAMPOS_ORDEN.contains(campo.getProperty())) {
                throw new PeticionInvalidaException("sort", "Campo de ordenación no permitido");
            }
            return campo.getProperty().equals("anio") ? campo.withProperty("anyo") : campo;
        }).toList());
        if (orden.getOrderFor("id") == null) {
            orden = orden.and(Sort.by("id"));
        }
        return cuotaRepository.findByIdSocio_Id(
                idSocio, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), orden))
                .map(cuotaMapper::toResponse);
    }

    public List<SocioMorosoResponse> pendientes(PeriodoCuotasRequest periodo) {
        return cuotaRepository.buscarSociosConCuotaPendiente(
                periodo.mes().shortValue(), periodo.anio().shortValue(), EstadoCuota.PENDIENTE);
    }
}
