

/**
 * Tipos de recurso con varias instancias.
 * En la admisión se reservan los máximos declarados en el PCB
 * solo si hay instancias suficientes; si no, el proceso espera.
 */
public final class GestorRecursos {
    public static final String[] NOMBRES = {"Impresora", "Disco", "Puerto"};

    private final int[] total;
    private final int[] disponible;

    public GestorRecursos(int[] total) {
        if (total == null || total.length != NOMBRES.length) {
            throw new IllegalArgumentException("Se requieren 3 tipos de recurso.");
        }
        this.total = total.clone();
        this.disponible = total.clone();
        for (int valor : total) {
            if (valor < 0) {
                throw new IllegalArgumentException("Las instancias de un recurso no pueden ser negativas.");
            }
        }
    }

    public void reiniciar() {
        System.arraycopy(total, 0, disponible, 0, total.length);
    }

    public boolean cabe(Proceso proceso) {
        int[] max = proceso.getMaxRecursos();
        if (max.length != total.length) {
            return false;
        }
        for (int i = 0; i < total.length; i++) {
            if (max[i] > disponible[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean excedeCapacidad(int[] maximos) {
        for (int i = 0; i < total.length; i++) {
            if (maximos[i] > total[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean reservar(Proceso proceso) {
        if (!cabe(proceso)) {
            return false;
        }
        int[] max = proceso.getMaxRecursos();
        int[] asignados = proceso.recursosAsignadosInterno();
        for (int i = 0; i < total.length; i++) {
            disponible[i] -= max[i];
            asignados[i] = max[i];
        }
        return true;
    }

    public void liberar(Proceso proceso) {
        int[] asignados = proceso.recursosAsignadosInterno();
        for (int i = 0; i < total.length && i < asignados.length; i++) {
            disponible[i] += asignados[i];
            if (disponible[i] > total[i]) {
                disponible[i] = total[i];
            }
            asignados[i] = 0;
        }
    }

    public int getTotal(int tipo) { return total[tipo]; }
    public int getDisponible(int tipo) { return disponible[tipo]; }
    public int tipos() { return NOMBRES.length; }

    public String resumen() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NOMBRES.length; i++) {
            if (i > 0) sb.append("   ");
            sb.append(NOMBRES[i]).append(": ").append(total[i] - disponible[i]).append("/").append(total[i]);
        }
        return sb.toString();
    }
}
