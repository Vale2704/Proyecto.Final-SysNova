
public enum AlgoritmoMemoria {
    FIRST_FIT("First Fit"),
    BEST_FIT("Best Fit"),
    WORST_FIT("Worst Fit");

    private final String etiqueta;

    AlgoritmoMemoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
