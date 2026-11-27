package es.safareyes.gimnasio.exception;

public class PeticionInvalidaException extends RuntimeException {
    private final String campo;

    public PeticionInvalidaException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
