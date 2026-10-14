
/**
 * Registro de un cambio de estado. La causa explica por qué ocurrió
 * (fin de quantum, E/S, espera de recurso, finalización, etc.).
 */
public final class Transicion {
    private final int tiempo;
    private final int pid;
    private final String proceso;
    private final EstadoProceso desde;
    private final EstadoProceso hacia;
    private final String causa;

    public Transicion(int tiempo, int pid, String proceso,
                      EstadoProceso desde, EstadoProceso hacia, String causa) {
        this.tiempo = tiempo;
        this.pid = pid;
        this.proceso = proceso;
        this.desde = desde;
        this.hacia = hacia;
        this.causa = causa;
    }

    public int getTiempo() { return tiempo; }
    public int getPid() { return pid; }
    public String getProceso() { return proceso; }
    public EstadoProceso getDesde() { return desde; }
    public EstadoProceso getHacia() { return hacia; }
    public String getCausa() { return causa; }

    public String getDesdeTexto() {
        return desde == null ? "—" : desde.getEtiqueta();
    }

    public String getHaciaTexto() {
        return hacia == null ? "—" : hacia.getEtiqueta();
    }
}
