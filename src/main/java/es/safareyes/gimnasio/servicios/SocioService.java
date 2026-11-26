package es.safareyes.gimnasio.servicios;

import es.safareyes.gimnasio.dto.request.SocioRequest;
import es.safareyes.gimnasio.modelos.Plan;
import es.safareyes.gimnasio.modelos.Socio;
import es.safareyes.gimnasio.repositorios.IPlanRepository;
import es.safareyes.gimnasio.repositorios.ISocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocioService {
    private final ISocioRepository socioRepository;

    private final IPlanRepository planRepository;

    public Optional<SocioRequest> getSociosById(Long id) {
        return socioRepository.findById(id).map(this::convertToDto);
    }

    public SocioRequest createSocio(SocioRequest request) {
        Plan plan = planRepository.findById(request.idPlan())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El plan indicado no existe"));

        Socio socio = new Socio();
        socio.setDni(request.dni());
        socio.setNombre(request.nombre());
        socio.setApellidos(request.apellidos());
        socio.setEmail(request.email());
        socio.setTelefono(request.telefono());
        socio.setMatricula(request.matricula());
        socio.setPlan(plan);
        socio.setFechaAlta(LocalDate.now());
        socio.setActivo(true);

        return convertToDto(socioRepository.save(socio));
    }


    private SocioRequest convertToDto(Socio socio) {
        return new SocioRequest(
                socio.getDni(),
                socio.getNombre(),
                socio.getApellidos(),
                socio.getEmail(),
                socio.getTelefono(),
                socio.getMatricula(),
                socio.getPlan() == null ? null : socio.getPlan().getId()
        );
    }
}
