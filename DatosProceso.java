
/** Definición de un proceso leída de un formulario, CSV o JSON. */
public final class DatosProceso {
    private final String nombre;
    private final int prioridad;
    private final int llegada;
    private final int rafaga;
    private final int paginas;
    private final int rafagaES;
    private final int[] recursos;

    public DatosProceso(String nombre, int prioridad, int llegada, int rafaga,
                        int paginas, int rafagaES, int[] recursos) {
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.llegada = llegada;
        this.rafaga = rafaga;
        this.paginas = paginas;
        this.rafagaES = rafagaES;
        this.recursos = recursos.clone();
    }

    public String getNombre() { return nombre; }
    public int getPrioridad() { return prioridad; }
    public int getLlegada() { return llegada; }
    public int getRafaga() { return rafaga; }
    public int getPaginas() { return paginas; }
    public int getRafagaES() { return rafagaES; }
    public int[] getRecursos() { return recursos.clone(); }
}
