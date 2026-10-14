
/** Mensaje entregado por buzón directo entre dos procesos. */
public final class Mensaje {
    private final int emisorPid;
    private final String emisor;
    private final int receptorPid;
    private final String texto;
    private final int tiempo;

    public Mensaje(int emisorPid, String emisor, int receptorPid, String texto, int tiempo) {
        this.emisorPid = emisorPid;
        this.emisor = emisor;
        this.receptorPid = receptorPid;
        this.texto = texto;
        this.tiempo = tiempo;
    }

    public int getEmisorPid() { return emisorPid; }
    public String getEmisor() { return emisor; }
    public int getReceptorPid() { return receptorPid; }
    public String getTexto() { return texto; }
    public int getTiempo() { return tiempo; }

    @Override
    public String toString() {
        return "P" + emisorPid + " → P" + receptorPid + ": " + texto;
    }
}
