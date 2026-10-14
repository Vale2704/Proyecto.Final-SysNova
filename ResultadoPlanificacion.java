

import java.util.List;

/** Métricas y diagrama producidos por una ejecución de la carga. */
public final class ResultadoPlanificacion {
    private final AlgoritmoCPU algoritmo;
    private final int quantum;
    private final int intervaloAging;
    private final List<EntradaGantt> gantt;
    private final List<Proceso> procesos;
    private final List<Transicion> transiciones;
    private final double esperaPromedio;
    private final double retornoPromedio;
    private final double respuestaPromedio;
    private final double utilizacionCpu;
    private final double throughput;
    private final int tiempoTotal;
    private final int completados;
    private final String mensaje;

    public ResultadoPlanificacion(AlgoritmoCPU algoritmo, int quantum, int intervaloAging,
                                  List<EntradaGantt> gantt, List<Proceso> procesos,
                                  List<Transicion> transiciones,
                                  double esperaPromedio, double retornoPromedio, double respuestaPromedio,
                                  double utilizacionCpu, double throughput,
                                  int tiempoTotal, int completados, String mensaje) {
        this.algoritmo = algoritmo;
        this.quantum = quantum;
        this.intervaloAging = intervaloAging;
        this.gantt = gantt;
        this.procesos = procesos;
        this.transiciones = transiciones;
        this.esperaPromedio = esperaPromedio;
        this.retornoPromedio = retornoPromedio;
        this.respuestaPromedio = respuestaPromedio;
        this.utilizacionCpu = utilizacionCpu;
        this.throughput = throughput;
        this.tiempoTotal = tiempoTotal;
        this.completados = completados;
        this.mensaje = mensaje == null ? "" : mensaje;
    }

    public AlgoritmoCPU getAlgoritmo() { return algoritmo; }
    public int getQuantum() { return quantum; }
    public int getIntervaloAging() { return intervaloAging; }
    public List<EntradaGantt> getGantt() { return gantt; }
    public List<Proceso> getProcesos() { return procesos; }
    public List<Transicion> getTransiciones() { return transiciones; }
    public double getEsperaPromedio() { return esperaPromedio; }
    public double getRetornoPromedio() { return retornoPromedio; }
    public double getRespuestaPromedio() { return respuestaPromedio; }
    public double getUtilizacionCpu() { return utilizacionCpu; }
    public double getThroughput() { return throughput; }
    public int getTiempoTotal() { return tiempoTotal; }
    public int getCompletados() { return completados; }
    public String getMensaje() { return mensaje; }

    public String firmaGantt() {
        StringBuilder sb = new StringBuilder();
        for (EntradaGantt e : gantt) {
            if (e.getPid() == 0) {
                continue;
            }
            sb.append(e.getNombre()).append(':').append(e.getInicio()).append('-').append(e.getFin()).append(';');
        }
        return sb.toString();
    }
}
