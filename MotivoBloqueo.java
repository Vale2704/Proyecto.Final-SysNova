
/** Causa por la que un proceso permanece en Bloqueado. */
public enum MotivoBloqueo {
    NINGUNO(""),
    MEMORIA("espera de memoria"),
    RECURSO("espera de recurso"),
    ES("E/S"),
    SEMAFORO("espera de semáforo"),
    MENSAJE("espera de mensaje");

    private final String etiqueta;

    MotivoBloqueo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
