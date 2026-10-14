
import java.util.List;

/** Cargas integradas para que las demostraciones no dependan de la carpeta de trabajo. */
public final class CargasEjemplo {
    private CargasEjemplo() {}

    /** Ejemplo clásico para contrastar FCFS, SJF, SRTF, Round Robin y prioridad. */
    public static List<DatosProceso> referencia() {
        return List.of(
                dato("P1", 2, 0, 8, 2, 0, 0, 0, 0),
                dato("P2", 3, 1, 4, 2, 0, 0, 0, 0),
                dato("P3", 1, 2, 9, 2, 0, 0, 0, 0),
                dato("P4", 4, 3, 5, 2, 0, 0, 0, 0)
        );
    }

    /** Misma idea de multiprogramación, con ráfagas de E/S que pasan por Bloqueado. */
    public static List<DatosProceso> conEntradaSalida() {
        return List.of(
                dato("Editor", 3, 0, 6, 4, 2, 1, 0, 0),
                dato("Compilador", 5, 1, 4, 5, 1, 0, 1, 0),
                dato("Navegador", 2, 3, 7, 4, 0, 0, 0, 1),
                dato("Antivirus", 4, 4, 3, 3, 2, 1, 0, 0),
                dato("Descarga", 1, 5, 5, 4, 3, 0, 1, 1)
        );
    }

    /**
     * Pensada para 16 páginas y una sola impresora.
     * MemB espera la impresora; MemC no cabe en las páginas libres y espera memoria.
     */
    public static List<DatosProceso> memoriaYRecursos() {
        return List.of(
                dato("MemA", 1, 0, 3, 8, 0, 1, 0, 0),
                dato("MemB", 1, 0, 3, 8, 0, 1, 0, 0),
                dato("MemC", 1, 0, 3, 10, 0, 0, 0, 0)
        );
    }

    private static DatosProceso dato(String nombre, int prioridad, int llegada, int rafaga,
                                     int paginas, int es, int r1, int r2, int r3) {
        return new DatosProceso(nombre, prioridad, llegada, rafaga, paginas, es, new int[]{r1, r2, r3});
    }
}
