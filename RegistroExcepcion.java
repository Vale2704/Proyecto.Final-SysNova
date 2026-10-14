
/** Excepción capturada por el manejador del sistema operativo simulado. */
public final class RegistroExcepcion {
    private final int tiempo;
    private final int pid;
    private final String proceso;
    private final String tipo;
    private final String mensaje;
    private final String manejo;

    public RegistroExcepcion(int tiempo, int pid, String proceso, String tipo, String mensaje, String manejo) {
        this.tiempo = tiempo;
        this.pid = pid;
        this.proceso = proceso;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.manejo = manejo;
    }

    public int getTiempo() { return tiempo; }
    public int getPid() { return pid; }
    public String getProceso() { return proceso; }
    public String getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public String getManejo() { return manejo; }
}
