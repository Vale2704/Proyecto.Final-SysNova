
/** Tramo continuo de CPU asignado a un proceso, o a la CPU ociosa si pid es 0. */
public final class EntradaGantt {
    private final int pid;
    private final String nombre;
    private final int inicio;
    private int fin;

    public EntradaGantt(int pid, String nombre, int inicio, int fin) {
        this.pid = pid;
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
    }

    public int getPid() { return pid; }
    public String getNombre() { return nombre; }
    public int getInicio() { return inicio; }
    public int getFin() { return fin; }
    public int getDuracion() { return fin - inicio; }

    public void setFin(int fin) {
        this.fin = fin;
    }
}
