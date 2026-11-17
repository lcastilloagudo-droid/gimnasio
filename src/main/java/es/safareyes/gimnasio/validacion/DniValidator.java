package es.safareyes.gimnasio.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;

public class DniValidator implements ConstraintValidator<DniValido, String> {
    private static final String LETRAS = "TRWAGMYFPDXBNJZSQVHLCKE";

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            // De la obligatoriedad se encarga @NotBlank.
            return true;
        }
        String dni = valor.toUpperCase(Locale.ROOT);
        if (!dni.matches("[0-9]{8}[A-Z]")) {
            return false;
        }
        int numero = Integer.parseInt(dni.substring(0, 8));
        return dni.charAt(8) == LETRAS.charAt(numero % 23);
    }
}
