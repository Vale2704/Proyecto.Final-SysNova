
/** Se lanza cuando un proceso direcciona una página que no le pertenece. */
public class AccesoMemoriaInvalidoException extends RuntimeException {
    public AccesoMemoriaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
