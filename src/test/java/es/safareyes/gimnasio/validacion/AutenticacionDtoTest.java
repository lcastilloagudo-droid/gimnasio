package es.safareyes.gimnasio.validacion;

import es.safareyes.gimnasio.dto.request.LoginRequest;
import es.safareyes.gimnasio.dto.request.RegistroRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutenticacionDtoTest {

    @Test
    void validaCredencialesYLetraDeControlDelDni() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validador = factory.getValidator();
            assertThat(validador.validate(new LoginRequest(" ", "")))
                    .extracting(this::campo).containsExactlyInAnyOrder("nomUsuario", "password");
            assertThat(validador.validate(new RegistroRequest("12345678A", "s".repeat(101), "prueba")))
                    .extracting(this::campo).containsExactlyInAnyOrder("dni", "nomUsuario");
            assertThat(validador.validate(new RegistroRequest("12345678z", "socio", "prueba"))).isEmpty();
        }
    }

    private String campo(ConstraintViolation<?> error) {
        return error.getPropertyPath().toString();
    }
}
