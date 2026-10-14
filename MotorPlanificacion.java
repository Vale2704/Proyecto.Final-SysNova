

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reloj discreto de la multiprogramación.
 * En cada unidad de tiempo: admisión, expropiación, despacho, ejecución,
 * contabilidad de espera y las transiciones de fin de quantum, E/S o finalización.
 *
 * Convención de cola en Round Robin y FCFS: las llegadas se encolan al inicio
 * de su instante; el proceso que agota el quantum se va al final al cerrar la unidad.
 */
public final class MotorPlanificacion {
    private static final int TOPE_PRIORIDAD = 30;
    private static final int TOPE_TIEMPO = 100_000;

    private final List<Proceso> procesos;
    private final GestorMemoria memoria;
    private final GestorRecursos recursos;
    private final List<Transicion> transiciones;
    private final AlgoritmoCPU algoritmo;
    private final int quantum;
    private final int intervaloAging;
    private final boolean limitarMemoriaRecursos;

    private final ArrayDeque<Proceso> cola = new ArrayDeque<>();
    private final List<EntradaGantt> gantt = new ArrayList<>();

    private int tiempo;
    private Proceso enCpu;
    private int quantumConsumido;
    private String mensaje = "";

    public MotorPlanificacion(List<Proceso> procesos, GestorMemoria memoria, GestorRecursos recursos,
                              List<Transicion> transiciones, AlgoritmoCPU algoritmo,
                              int quantum, int intervaloAging, boolean limitarMemoriaRecursos) {
        if (quantum < 1) {
            throw new IllegalArgumentException("El quantum debe ser un entero mayor o igual que 1.");
        }
        if (intervaloAging < 1) {
            throw new IllegalArgumentException("El intervalo de envejecimiento debe ser mayor o igual que 1.");
        }
        this.procesos = procesos;
        this.memoria = memoria;
        this.recursos = recursos;
        this.transiciones = transiciones;
        this.algoritmo = algoritmo;
        this.quantum = quantum;
        this.intervaloAging = intervaloAging;
        this.limitarMemoriaRecursos = limitarMemoriaRecursos;
    }

    public static ResultadoPlanificacion ejecutar(List<Proceso> procesos, int paginas, AlgoritmoMemoria algMem,
                                                  int[] totalesRecursos, AlgoritmoCPU algoritmo,
                                                  int quantum, int intervaloAging, boolean limitar) {
        GestorMemoria memoria = new GestorMemoria(paginas, algMem);
        GestorRecursos recursos = new GestorRecursos(totalesRecursos);
        List<Transicion> transiciones = new ArrayList<>();
        MotorPlanificacion motor = new MotorPlanificacion(
                procesos, memoria, recursos, transiciones, algoritmo, quantum, intervaloAging, limitar);
        int guardia = 0;
        PasoSimulacion paso;
        do {
            paso = motor.paso();
            guardia++;
        } while (paso == PasoSimulacion.AVANZO && guardia < TOPE_TIEMPO);
        return motor.vista();
    }

    public int getTiempo() { return tiempo; }
    public Proceso getEnCpu() { return enCpu; }
    public String getMensaje() { return mensaje; }
    public AlgoritmoCPU getAlgoritmo() { return algoritmo; }

    public void retirar(Proceso proceso) {
        cola.remove(proceso);
        if (enCpu == proceso) {
            enCpu = null;
            quantumConsumido = 0;
        }
    }

    public PasoSimulacion paso() {
        if (procesos.isEmpty() || todosTerminados()) {
            return PasoSimulacion.FIN;
        }
        if (tiempo > TOPE_TIEMPO) {
            mensaje = "Se alcanzó el límite de tiempo de la simulación.";
            return PasoSimulacion.ATASCO;
        }

        sincronizarCola();
        if (enCpu != null && enCpu.getEstado() != EstadoProceso.EJECUCION) {
            enCpu = null;
            quantumConsumido = 0;
        }
        admitir();
        if (todosTerminados()) {
            return PasoSimulacion.FIN;
        }
        if (enCpu != null && debeExpropiar()) {
            expropiar();
        }
        if (enCpu == null) {
            despachar();
        }

        if (enCpu == null && cola.isEmpty() && !hayEntradaSalida()) {
            int siguiente = siguienteLlegada();
            if (siguiente == Integer.MAX_VALUE) {
                mensaje = describirAtasco();
                return PasoSimulacion.ATASCO;
            }
            int delta = siguiente - tiempo;
            for (Proceso p : procesos) {
                if (p.getEstado() == EstadoProceso.BLOQUEADO) {
                    p.sumarBloqueado(delta);
                }
            }
            registrar(0, "OCIOSO", tiempo, siguiente);
            tiempo = siguiente;
            return PasoSimulacion.AVANZO;
        }

        List<Proceso> esperaDurante = new ArrayList<>(cola);
        List<Proceso> bloqueadosDurante = new ArrayList<>();
        for (Proceso p : procesos) {
            if (p.getEstado() == EstadoProceso.BLOQUEADO) {
                bloqueadosDurante.add(p);
            }
        }

        if (enCpu != null) {
            enCpu.consumirCpu();
            quantumConsumido++;
            registrar(enCpu.getPid(), enCpu.getNombre(), tiempo, tiempo + 1);
        } else {
            registrar(0, "OCIOSO", tiempo, tiempo + 1);
        }

        int marca = tiempo + 1;
        for (Proceso p : esperaDurante) {
            p.sumarEspera(1);
            if (algoritmo == AlgoritmoCPU.PRIORIDAD_AGING) {
                int antes = p.getPrioridad();
                int nueva = p.envejecer(intervaloAging, TOPE_PRIORIDAD);
                if (nueva > 0) {
                    transiciones.add(new Transicion(marca, p.getPid(), p.getNombre(),
                            p.getEstado(), p.getEstado(),
                            "envejecimiento: prioridad " + antes + " → " + nueva));
                }
            }
        }

        for (Proceso p : bloqueadosDurante) {
            p.sumarBloqueado(1);
            if (p.getMotivo() == MotivoBloqueo.ES) {
                p.consumirES();
                if (p.getIoRestante() == 0) {
                    if (p.getRestante() == 0) {
                        p.cambiarEstado(EstadoProceso.LISTO, "fin de E/S", marca, transiciones);
                        finalizarNormal(p, marca);
                    } else {
                        ponerEnListo(p, "fin de E/S", marca);
                    }
                }
            }
        }

        if (enCpu != null && enCpu.debeIniciarES()) {
            Proceso p = enCpu;
            enCpu = null;
            quantumConsumido = 0;
            p.iniciarES();
            p.cambiarEstado(EstadoProceso.BLOQUEADO, "E/S", marca, transiciones);
        } else if (enCpu != null && enCpu.getRestante() == 0) {
            Proceso p = enCpu;
            enCpu = null;
            quantumConsumido = 0;
            finalizarNormal(p, marca);
        } else if (enCpu != null && algoritmo == AlgoritmoCPU.ROUND_ROBIN && quantumConsumido >= quantum) {
            Proceso p = enCpu;
            enCpu = null;
            quantumConsumido = 0;
            ponerEnListo(p, "fin de quantum", marca);
        }

        tiempo = marca;
        if (todosTerminados()) {
            return PasoSimulacion.FIN;
        }
        return PasoSimulacion.AVANZO;
    }

    public ResultadoPlanificacion vista() {
        int ocupado = 0;
        List<EntradaGantt> copiaGantt = new ArrayList<>();
        for (EntradaGantt e : gantt) {
            copiaGantt.add(new EntradaGantt(e.getPid(), e.getNombre(), e.getInicio(), e.getFin()));
            if (e.getPid() != 0) {
                ocupado += e.getDuracion();
            }
        }
        int completados = 0;
        double sumaEspera = 0;
        double sumaRetorno = 0;
        double sumaRespuesta = 0;
        for (Proceso p : procesos) {
            if (!p.isFinalizoNormal()) {
                continue;
            }
            completados++;
            sumaEspera += p.getTiempoEspera();
            sumaRetorno += p.getRetorno();
            sumaRespuesta += p.getRespuesta();
        }
        double divisor = completados == 0 ? 1 : completados;
        double util = tiempo == 0 ? 0 : (100.0 * ocupado / tiempo);
        double throughput = tiempo == 0 ? 0 : (completados / (double) tiempo);
        return new ResultadoPlanificacion(
                algoritmo, quantum, intervaloAging,
                copiaGantt, procesos, new ArrayList<>(transiciones),
                sumaEspera / divisor, sumaRetorno / divisor, sumaRespuesta / divisor,
                util, throughput, tiempo, completados, mensaje);
    }

    private void sincronizarCola() {
        cola.removeIf(p -> p.getEstado() != EstadoProceso.LISTO);
        for (Proceso p : procesos) {
            if (p.getEstado() == EstadoProceso.LISTO && !cola.contains(p)) {
                cola.addLast(p);
            }
        }
    }

    private void admitir() {
        List<Proceso> candidatos = new ArrayList<>();
        for (Proceso p : procesos) {
            boolean llego = p.getEstado() == EstadoProceso.NUEVO && p.getLlegada() <= tiempo;
            boolean reintento = p.getEstado() == EstadoProceso.BLOQUEADO
                    && (p.getMotivo() == MotivoBloqueo.MEMORIA || p.getMotivo() == MotivoBloqueo.RECURSO);
            if (llego || reintento) {
                candidatos.add(p);
            }
        }
        candidatos.sort(Comparator.comparingInt(Proceso::getPid));
        for (Proceso p : candidatos) {
            intentarAdmitir(p);
        }
    }

    private void intentarAdmitir(Proceso p) {
        boolean hayMemoria = memoria.cabe(p);
        boolean hayRecursos = recursos.cabe(p);
        if (limitarMemoriaRecursos && !hayMemoria) {
            bloquear(p, MotivoBloqueo.MEMORIA, "espera de memoria");
            return;
        }
        if (limitarMemoriaRecursos && !hayRecursos) {
            bloquear(p, MotivoBloqueo.RECURSO, "espera de recurso");
            return;
        }
        if (hayMemoria) {
            memoria.asignar(p);
        }
        if (hayRecursos) {
            recursos.reservar(p);
        }
        String causa = "admisión";
        if (p.getEstado() == EstadoProceso.BLOQUEADO && p.getMotivo() == MotivoBloqueo.MEMORIA) {
            causa = "memoria asignada";
        } else if (p.getEstado() == EstadoProceso.BLOQUEADO && p.getMotivo() == MotivoBloqueo.RECURSO) {
            causa = "recurso asignado";
        }
        ponerEnListo(p, causa, tiempo);
    }

    private void ponerEnListo(Proceso p, String causa, int marca) {
        p.cambiarEstado(EstadoProceso.LISTO, causa, marca, transiciones);
        p.setMotivo(MotivoBloqueo.NINGUNO);
        if (!cola.contains(p)) {
            cola.addLast(p);
        }
    }

    private void bloquear(Proceso p, MotivoBloqueo motivo, String causa) {
        if (p.getEstado() == EstadoProceso.BLOQUEADO && p.getMotivo() == motivo) {
            return;
        }
        cola.remove(p);
        if (enCpu == p) {
            enCpu = null;
            quantumConsumido = 0;
        }
        p.setMotivo(motivo);
        p.cambiarEstado(EstadoProceso.BLOQUEADO, causa, tiempo, transiciones);
    }

    private void despachar() {
        Proceso elegido = seleccionar();
        if (elegido == null) {
            return;
        }
        elegido.marcarInicioCpu(tiempo);
        elegido.cambiarEstado(EstadoProceso.EJECUCION, "despacho", tiempo, transiciones);
        enCpu = elegido;
        quantumConsumido = 0;
    }

    private Proceso seleccionar() {
        if (cola.isEmpty()) {
            return null;
        }
        if (algoritmo == AlgoritmoCPU.FCFS || algoritmo == AlgoritmoCPU.ROUND_ROBIN) {
            return cola.pollFirst();
        }
        Proceso mejor = null;
        for (Proceso candidato : cola) {
            if (mejor == null || esMejor(candidato, mejor)) {
                mejor = candidato;
            }
        }
        cola.remove(mejor);
        return mejor;
    }

    private boolean esMejor(Proceso a, Proceso b) {
        int cmp;
        if (algoritmo == AlgoritmoCPU.SJF || algoritmo == AlgoritmoCPU.SRTF) {
            cmp = Integer.compare(a.getRafagaActual(), b.getRafagaActual());
        } else {
            cmp = Integer.compare(b.getPrioridad(), a.getPrioridad());
        }
        if (cmp != 0) {
            return cmp < 0;
        }
        if (a.getLlegada() != b.getLlegada()) {
            return a.getLlegada() < b.getLlegada();
        }
        return a.getPid() < b.getPid();
    }

    private boolean debeExpropiar() {
        if (enCpu == null || cola.isEmpty()) {
            return false;
        }
        if (algoritmo == AlgoritmoCPU.SRTF) {
            for (Proceso p : cola) {
                if (p.getRafagaActual() < enCpu.getRafagaActual()) {
                    return true;
                }
            }
        } else if (algoritmo == AlgoritmoCPU.PRIORIDAD_AGING) {
            for (Proceso p : cola) {
                if (p.getPrioridad() > enCpu.getPrioridad()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void expropiar() {
        String causa = algoritmo == AlgoritmoCPU.SRTF
                ? "expropiación: menor tiempo restante (SRTF)"
                : "expropiación: mayor prioridad";
        Proceso actual = enCpu;
        enCpu = null;
        quantumConsumido = 0;
        ponerEnListo(actual, causa, tiempo);
    }

    private void finalizarNormal(Proceso p, int marca) {
        if (p.tieneMemoria()) {
            memoria.liberar(p);
        }
        recursos.liberar(p);
        p.setFin(marca);
        p.setFinalizoNormal(true);
        p.cambiarEstado(EstadoProceso.TERMINADO, "finalización", marca, transiciones);
        cola.remove(p);
        if (enCpu == p) {
            enCpu = null;
        }
    }

    private boolean todosTerminados() {
        for (Proceso p : procesos) {
            if (p.getEstado() != EstadoProceso.TERMINADO) {
                return false;
            }
        }
        return true;
    }

    private boolean hayEntradaSalida() {
        for (Proceso p : procesos) {
            if (p.getEstado() == EstadoProceso.BLOQUEADO && p.getMotivo() == MotivoBloqueo.ES && p.getIoRestante() > 0) {
                return true;
            }
        }
        return false;
    }

    private int siguienteLlegada() {
        int siguiente = Integer.MAX_VALUE;
        for (Proceso p : procesos) {
            if (p.getEstado() == EstadoProceso.NUEVO && p.getLlegada() > tiempo) {
                siguiente = Math.min(siguiente, p.getLlegada());
            }
        }
        return siguiente;
    }

    private String describirAtasco() {
        for (Proceso p : procesos) {
            if (p.getEstado() == EstadoProceso.BLOQUEADO
                    && (p.getMotivo() == MotivoBloqueo.SEMAFORO || p.getMotivo() == MotivoBloqueo.MENSAJE)) {
                return "La CPU quedó libre y los procesos restantes esperan un semáforo o un mensaje.";
            }
        }
        return "No se puede continuar: hay procesos en espera de memoria o recursos que no se liberarán.";
    }

    private void registrar(int pid, String nombre, int desde, int hasta) {
        if (hasta <= desde) {
            return;
        }
        if (!gantt.isEmpty()) {
            EntradaGantt ultima = gantt.get(gantt.size() - 1);
            if (ultima.getPid() == pid && ultima.getFin() == desde) {
                ultima.setFin(hasta);
                return;
            }
        }
        gantt.add(new EntradaGantt(pid, nombre, desde, hasta));
    }
}
