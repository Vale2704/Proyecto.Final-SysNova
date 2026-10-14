
import java.util.Arrays;

/**
 * Bloque de control de proceso (PCB).
 * Conserva la definición original y el estado de ejecución por separado,
 * de modo que la misma carga pueda reiniciarse con cada algoritmo.
 */
public final class Proceso {
    private final int pid;
    private final String nombre;
    private final int prioridadBase;
    private final int llegada;
    private final int rafaga;
    private final int paginas;
    private final int rafagaES;
    private final int[] maxRecursos;

    private int prioridad;
    private int restante;
    private boolean ioIniciada;
    private int ioRestante;
    private EstadoProceso estado;
    private MotivoBloqueo motivo;
    private String ultimaCausa;
    private int tiempoEspera;
    private int tiempoBloqueado;
    private int inicioCpu;
    private int fin;
    private boolean finalizoNormal;
    private int ticksAging;
    private boolean memoriaAsignada;
    private int baseMemoria;
    private final int[] recursosAsignados;
    private String ultimoMensaje;

    public Proceso(int pid, String nombre, int prioridad, int llegada, int rafaga,
                   int paginas, int rafagaES, int[] maxRecursos) {
        this.pid = pid;
        this.nombre = nombre;
        this.prioridadBase = prioridad;
        this.llegada = llegada;
        this.rafaga = rafaga;
        this.paginas = paginas;
        this.rafagaES = rafagaES;
        this.maxRecursos = maxRecursos.clone();
        this.recursosAsignados = new int[maxRecursos.length];
        reiniciarEjecucion();
    }

    public Proceso copiaLimpia() {
        return new Proceso(pid, nombre, prioridadBase, llegada, rafaga, paginas, rafagaES, maxRecursos);
    }

    public void reiniciarEjecucion() {
        prioridad = prioridadBase;
        restante = rafaga;
        ioIniciada = false;
        ioRestante = 0;
        estado = EstadoProceso.NUEVO;
        motivo = MotivoBloqueo.NINGUNO;
        ultimaCausa = "creación de proceso";
        tiempoEspera = 0;
        tiempoBloqueado = 0;
        inicioCpu = -1;
        fin = -1;
        finalizoNormal = false;
        ticksAging = 0;
        memoriaAsignada = false;
        baseMemoria = -1;
        Arrays.fill(recursosAsignados, 0);
        ultimoMensaje = "";
    }

    public void cambiarEstado(EstadoProceso nuevo, String causa, int tiempo, java.util.List<Transicion> bitacora) {
        if (estado == nuevo) {
            ultimaCausa = causa;
            return;
        }
        EstadoProceso anterior = estado;
        estado = nuevo;
        ultimaCausa = causa;
        if (nuevo != EstadoProceso.BLOQUEADO) {
            motivo = MotivoBloqueo.NINGUNO;
        }
        if (bitacora != null) {
            bitacora.add(new Transicion(tiempo, pid, nombre, anterior, nuevo, causa));
        }
    }

    /** Unidades de CPU de la ráfaga actual (hasta la próxima E/S o hasta terminar). */
    public int getRafagaActual() {
        if (rafagaES > 0 && !ioIniciada) {
            int hastaIo = getPuntoEs() - cpuEjecutada();
            if (hastaIo > 0) {
                return hastaIo;
            }
        }
        return restante;
    }

    /** Momento, en unidades de CPU ya consumidas, en el que el proceso pide E/S. */
    public int getPuntoEs() {
        if (rafagaES <= 0 || rafaga <= 1) {
            return rafaga;
        }
        return Math.max(1, rafaga / 2);
    }

    public boolean debeIniciarES() {
        return rafagaES > 0 && !ioIniciada && cpuEjecutada() >= getPuntoEs();
    }

    public int cpuEjecutada() {
        return rafaga - restante;
    }

    public void consumirCpu() {
        if (restante > 0) {
            restante--;
        }
    }

    public void iniciarES() {
        ioIniciada = true;
        ioRestante = rafagaES;
        motivo = MotivoBloqueo.ES;
    }

    public void consumirES() {
        if (ioRestante > 0) {
            ioRestante--;
        }
    }

    public void sumarEspera(int unidades) { tiempoEspera += unidades; }
    public void sumarBloqueado(int unidades) { tiempoBloqueado += unidades; }

    public int envejecer(int intervalo, int tope) {
        ticksAging++;
        if (ticksAging < intervalo) {
            return -1;
        }
        ticksAging = 0;
        if (prioridad >= tope) {
            return -1;
        }
        prioridad++;
        return prioridad;
    }

    public void marcarInicioCpu(int tiempo) {
        if (inicioCpu < 0) {
            inicioCpu = tiempo;
        }
    }

    public void asignarMemoria(int base) {
        this.baseMemoria = base;
        this.memoriaAsignada = true;
    }

    public void soltarMemoria() {
        this.baseMemoria = -1;
        this.memoriaAsignada = false;
    }

    public int getRetorno() {
        return fin < 0 ? -1 : fin - llegada;
    }

    public int getRespuesta() {
        return inicioCpu < 0 ? -1 : inicioCpu - llegada;
    }

    public String recursosMaxTexto() {
        return formatear(maxRecursos);
    }

    public String recursosAsignadosTexto() {
        return formatear(recursosAsignados);
    }

    private static String formatear(int[] valores) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(valores[i]);
        }
        return sb.append(")").toString();
    }

    public int getPid() { return pid; }
    public String getNombre() { return nombre; }
    public String getEtiqueta() { return "P" + pid; }
    public int getPrioridadBase() { return prioridadBase; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public int getLlegada() { return llegada; }
    public int getRafaga() { return rafaga; }
    public int getRestante() { return restante; }
    public int getPaginas() { return paginas; }
    public int getRafagaES() { return rafagaES; }
    public int getIoRestante() { return ioRestante; }
    public boolean isIoIniciada() { return ioIniciada; }
    public int[] getMaxRecursos() { return maxRecursos.clone(); }
    public int[] getRecursosAsignados() { return recursosAsignados.clone(); }
    public int[] recursosAsignadosInterno() { return recursosAsignados; }
    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivo() { return motivo; }
    public void setMotivo(MotivoBloqueo motivo) { this.motivo = motivo; }
    public String getUltimaCausa() { return ultimaCausa; }
    public int getTiempoEspera() { return tiempoEspera; }
    public int getTiempoBloqueado() { return tiempoBloqueado; }
    public int getInicioCpu() { return inicioCpu; }
    public int getFin() { return fin; }
    public void setFin(int fin) { this.fin = fin; }
    public boolean isFinalizoNormal() { return finalizoNormal; }
    public void setFinalizoNormal(boolean valor) { this.finalizoNormal = valor; }
    public boolean tieneMemoria() { return memoriaAsignada; }
    public int getBaseMemoria() { return baseMemoria; }
    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }

    @Override
    public String toString() {
        return getEtiqueta() + " " + nombre;
    }
}
