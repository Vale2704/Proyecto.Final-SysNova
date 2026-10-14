
/** Hueco o región ocupada de la memoria contigua, medida en páginas. */
public final class BloqueMemoria {
    private int inicio;
    private int tamano;
    private int pid;
    private String nombre;

    public BloqueMemoria(int inicio, int tamano, int pid, String nombre) {
        this.inicio = inicio;
        this.tamano = tamano;
        this.pid = pid;
        this.nombre = nombre;
    }

    public boolean estaLibre() { return pid < 0; }
    public int getInicio() { return inicio; }
    public int getTamano() { return tamano; }
    public int getPid() { return pid; }
    public String getNombre() { return nombre == null ? "" : nombre; }
    public int getFin() { return inicio + tamano; }

    public void setInicio(int inicio) { this.inicio = inicio; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public void ocupar(int pid, String nombre) {
        this.pid = pid;
        this.nombre = nombre;
    }
    public void liberar() {
        this.pid = -1;
        this.nombre = "";
    }
}
