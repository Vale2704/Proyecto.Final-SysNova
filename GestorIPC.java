

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Semáforos con nombre y buzones de mensajes por proceso receptor. */
public final class GestorIPC {
    private final Map<String, SemaforoSO> semaforos = new LinkedHashMap<>();
    private final Map<Integer, ArrayDeque<Mensaje>> buzones = new LinkedHashMap<>();
    private final Map<Integer, Proceso> esperandoMensaje = new LinkedHashMap<>();

    public void crearSemaforo(String nombre, int valorInicial) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El semáforo necesita un nombre.");
        }
        if (semaforos.containsKey(limpio)) {
            throw new IllegalArgumentException("Ya existe un semáforo llamado «" + limpio + "».");
        }
        semaforos.put(limpio, new SemaforoSO(limpio, valorInicial));
    }

    public SemaforoSO getSemaforo(String nombre) {
        SemaforoSO semaforo = semaforos.get(nombre);
        if (semaforo == null) {
            throw new IllegalArgumentException("No existe el semáforo «" + nombre + "».");
        }
        return semaforo;
    }

    public List<SemaforoSO> getSemaforos() {
        return new ArrayList<>(semaforos.values());
    }

    public boolean esperar(Proceso proceso, String nombre) {
        return getSemaforo(nombre).esperar(proceso);
    }

    public Proceso senalar(String nombre) {
        return getSemaforo(nombre).senalar();
    }

    /**
     * @return el receptor que estaba bloqueado esperando este mensaje, o null si se encoló.
     */
    public Proceso enviar(Proceso emisor, Proceso receptor, String texto, int tiempo) {
        Mensaje mensaje = new Mensaje(emisor.getPid(), emisor.getNombre(), receptor.getPid(), texto, tiempo);
        Proceso dormido = esperandoMensaje.remove(receptor.getPid());
        if (dormido != null) {
            dormido.setUltimoMensaje(texto);
            return dormido;
        }
        buzones.computeIfAbsent(receptor.getPid(), k -> new ArrayDeque<>()).addLast(mensaje);
        return null;
    }

    /** @return el mensaje recibido, o null si el proceso debe bloquearse. */
    public Mensaje recibir(Proceso receptor) {
        ArrayDeque<Mensaje> bandeja = buzones.get(receptor.getPid());
        if (bandeja != null && !bandeja.isEmpty()) {
            Mensaje mensaje = bandeja.pollFirst();
            receptor.setUltimoMensaje(mensaje.getTexto());
            return mensaje;
        }
        esperandoMensaje.put(receptor.getPid(), receptor);
        return null;
    }

    public void limpiarBandeja(int pid) {
        buzones.remove(pid);
        esperandoMensaje.remove(pid);
    }

    public int mensajesPendientes(int pid) {
        ArrayDeque<Mensaje> bandeja = buzones.get(pid);
        return bandeja == null ? 0 : bandeja.size();
    }

    public void retirar(Proceso proceso) {
        for (SemaforoSO semaforo : semaforos.values()) {
            semaforo.retirar(proceso);
        }
        esperandoMensaje.remove(proceso.getPid());
    }

    public void reiniciar() {
        for (SemaforoSO semaforo : semaforos.values()) {
            semaforo.reiniciar();
        }
        buzones.clear();
        esperandoMensaje.clear();
    }

    public void vaciar() {
        semaforos.clear();
        buzones.clear();
        esperandoMensaje.clear();
    }
}
