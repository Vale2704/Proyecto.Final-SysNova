
public enum AlgoritmoCPU {
    FCFS("FCFS"),
    SJF("SJF (no expropiativo)"),
    SRTF("SRTF (expropiativo)"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDAD_AGING("Prioridad con envejecimiento");

    private final String etiqueta;

    AlgoritmoCPU(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
