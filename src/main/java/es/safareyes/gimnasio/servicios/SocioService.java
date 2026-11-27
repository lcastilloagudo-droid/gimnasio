package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.request.SocioEstadoRequest;
import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.dto.request.SocioUpdateRequest;
import es.safareyes.gimnasio.dto.response.SocioResponse;
import es.safareyes.gimnasio.exception.ConflictoException;
import es.safareyes.gimnasio.exception.PeticionInvalidaException;
import es.safareyes.gimnasio.exception.RecursoNoEncontradoException;
import es.safareyes.gimnasio.exception.ReglaNegocioException;
import es.safareyes.gimnasio.mapper.SocioMapper;
import es.safareyes.gimnasio.modelos.Plan;
import es.safareyes.gimnasio.modelos.Socio;
import es.safareyes.gimnasio.repositorios.IPlanRepository;
import es.safareyes.gimnasio.repositorios.ISocioRepository;
import es.safareyes.gimnasio.repositorios.SocioSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SocioService {
    private final ISocioRepository socioRepository;
    private final IPlanRepository planRepository;
    private final SocioMapper socioMapper;

    private static final Set<String> CAMPOS_ORDEN = Set.of(
            "id", "dni", "nombre", "apellidos", "email", "fechaAlta", "activo");

    public SocioResponse getSocioById(Long id) {
        return socioMapper.toResponse(buscarSocio(id));
    }

    public Page<SocioResponse> listar(Boolean activo, Long idPlan, String texto,
                                     LocalDate fechaAltaDesde, LocalDate fechaAltaHasta,
                                     Pageable pageable) {
        if (fechaAltaDesde != null && fechaAltaHasta != null
                && fechaAltaDesde.isAfter(fechaAltaHasta)) {
            throw new PeticionInvalidaException("fechaAltaDesde", "La fecha inicial no puede ser posterior a la final");
        }
        pageable.getSort().forEach(orden -> {
            if (!CAMPOS_ORDEN.contains(orden.getProperty())) {
                throw new PeticionInvalidaException("sort", "Campo de ordenación no permitido");
            }
        });

        if (pageable.isPaged() && pageable.getSort().getOrderFor("id") == null) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    pageable.getSort().and(Sort.by("id")));
        }

        return socioRepository.findAll(
                SocioSpecifications.conFiltros(activo, idPlan, texto, fechaAltaDesde, fechaAltaHasta),
                pageable).map(socioMapper::toResponse);
    }

    @Transactional
    public SocioResponse createSocio(SocioRequest request) {
        String dni = request.dni().toUpperCase(Locale.ROOT);
        String email = request.email().toLowerCase(Locale.ROOT);

        if (socioRepository.existsByDniIgnoreCase(dni)) {
            throw new ConflictoException("Ya existe un socio con ese DNI");
        }
        if (socioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe un socio con ese email");
        }

        Plan plan = buscarPlan(request.idPlan());
        Socio socio = socioMapper.toEntity(request);
        socio.setDni(dni);
        socio.setEmail(email);
        socio.setPlan(plan);
        socio.setFechaAlta(LocalDate.now(ZoneId.of("Europe/Madrid")));
        socio.setActivo(plan != null);

        Socio guardado = socioRepository.saveAndFlush(socio);
        log.info("Socio creado: id={}", guardado.getId());
        return socioMapper.toResponse(guardado);
    }

    @Transactional
    public SocioResponse actualizar(Long id, SocioUpdateRequest request) {
        Socio socio = bloquearSocio(id);
        String email = request.email().toLowerCase(Locale.ROOT);
        if (socioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictoException("Ya existe otro socio con ese email");
        }

        if (socio.getActivo() && request.idPlan() == null) {
            throw new ReglaNegocioException("Un socio activo debe tener un plan");
        }
        Plan plan = buscarPlan(request.idPlan());
        socioMapper.actualizar(request, socio);
        socio.setEmail(email);
        // RN-09: se cambia el plan vigente, no las cuotas ya generadas.
        socio.setPlan(plan);
        Socio guardado = socioRepository.saveAndFlush(socio);
        log.info("Socio actualizado: id={}", id);
        return socioMapper.toResponse(guardado);
    }

    @Transactional
    public SocioResponse cambiarEstado(Long id, SocioEstadoRequest request) {
        Socio socio = bloquearSocio(id);
        if (request.activo() && socio.getPlan() == null) {
            throw new ReglaNegocioException("Asigna un plan antes de activar al socio");
        }
        socioMapper.cambiarEstado(request, socio);
        Socio guardado = socioRepository.saveAndFlush(socio);
        log.info("Estado del socio {} cambiado a {}", id, guardado.getActivo());
        return socioMapper.toResponse(guardado);
    }

    private Socio buscarSocio(Long id) {
        return socioRepository.buscarConPlan(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio con id " + id));
    }

    private Socio bloquearSocio(Long id) {
        return socioRepository.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio con id " + id));
    }

    private Plan buscarPlan(Long id) {
        if (id == null) {
            return null;
        }
        return planRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el plan con id " + id));
    }
}
