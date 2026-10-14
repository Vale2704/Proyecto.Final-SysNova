

import java.util.ArrayDeque;

/** Semáforo contador. La cola de espera se atiende en orden FIFO. */
public final class SemaforoSO {
    private final String nombre;
    private final int valorInicial;
    private int valor;
    private final ArrayDeque<Proceso> espera = new ArrayDeque<>();

    public SemaforoSO(String nombre, int valorInicial) {
        if (valorInicial < 0) {
            throw new IllegalArgumentException("El valor inicial del semáforo no puede ser negativo.");
        }
        this.nombre = nombre;
        this.valorInicial = valorInicial;
        this.valor = valorInicial;
    }

    public String getNombre() { return nombre; }
    public int getValor() { return valor; }
    public int getValorInicial() { return valorInicial; }
    public int getEnEspera() { return espera.size(); }

    public String procesosEnEspera() {
        if (espera.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (Proceso p : espera) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p.getEtiqueta());
        }
        return sb.toString();
    }

    /** @return true si el proceso adquirió el semáforo; false si quedó en la cola. */
    public boolean esperar(Proceso proceso) {
        if (valor > 0) {
            valor--;
            return true;
        }
        if (!espera.contains(proceso)) {
            espera.addLast(proceso);
        }
        return false;
    }

    /** @return el proceso despertado, o null si solo se incrementó el contador. */
    public Proceso senalar() {
        if (!espera.isEmpty()) {
            return espera.pollFirst();
        }
        valor++;
        return null;
    }

    public void retirar(Proceso proceso) {
        espera.remove(proceso);
    }

    public void reiniciar() {
        valor = valorInicial;
        espera.clear();
    }
}
