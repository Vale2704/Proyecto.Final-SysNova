

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fachada del sistema operativo simulado.
 * La interfaz gráfica habla solo con esta clase.
 */
public final class Sistema {
    public static final int KB_POR_PAGINA = 4;

    private final List<Proceso> procesos = new ArrayList<>();
    private final List<Transicion> transiciones = new ArrayList<>();
    private final List<String> bitacora = new ArrayList<>();
    private final List<RegistroExcepcion> excepciones = new ArrayList<>();
    private final List<Runnable> observadores = new ArrayList<>();
    private final GestorIPC ipc = new GestorIPC();
    private final ManejadorExcepciones manejador = new ManejadorExcepciones();

    private GestorMemoria memoria = new GestorMemoria(64, AlgoritmoMemoria.FIRST_FIT);
    private GestorRecursos recursos = new GestorRecursos(new int[]{2, 2, 2});
    private int siguientePid = 1;
    private AlgoritmoCPU algoritmo = AlgoritmoCPU.FCFS;
    private int quantum = 4;
    private int intervaloAging = 2;
    private boolean limitarMemoriaRecursos = true;
    private MotorPlanificacion motor;
    private ResultadoPlanificacion vista;
    private List<ResultadoPlanificacion> comparacion = List.of();

    public void observar(Runnable accion) {
        observadores.add(accion);
    }

    private void notificar() {
        for (Runnable observador : observadores) {
            observador.run();
        }
    }

    public void anotar(String texto) {
        bitacora.add(texto);
        if (bitacora.size() > 400) {
            bitacora.remove(0);
        }
        notificar();
    }

    public List<Proceso> getProcesos() { return procesos; }
    public List<Transicion> getTransiciones() { return transiciones; }
    public List<String> getBitacora() { return bitacora; }
    public List<RegistroExcepcion> getExcepciones() { return excepciones; }
    public GestorMemoria getMemoria() { return memoria; }
    public GestorRecursos getRecursos() { return recursos; }
    public GestorIPC getIpc() { return ipc; }
    public AlgoritmoCPU getAlgoritmo() { return algoritmo; }
    public int getQuantum() { return quantum; }
    public int getIntervaloAging() { return intervaloAging; }
    public boolean isLimitarMemoriaRecursos() { return limitarMemoriaRecursos; }
    public boolean tieneSimulacion() { return motor != null; }
    public ResultadoPlanificacion getVista() { return vista; }
    public List<ResultadoPlanificacion> getComparacion() { return comparacion; }
    public int getReloj() { return motor == null ? 0 : motor.getTiempo(); }

    public void setAlgoritmo(AlgoritmoCPU algoritmo) { this.algoritmo = algoritmo; }
    public void setQuantum(int quantum) {
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser al menos 1.");
        this.quantum = quantum;
    }
    public void setIntervaloAging(int intervaloAging) {
        if (intervaloAging < 1) throw new IllegalArgumentException("El intervalo de envejecimiento debe ser al menos 1.");
        this.intervaloAging = intervaloAging;
    }
    public void setLimitarMemoriaRecursos(boolean limitar) { this.limitarMemoriaRecursos = limitar; }

    public void setAlgoritmoMemoria(AlgoritmoMemoria algoritmo) {
        memoria.setAlgoritmo(algoritmo);
        anotar("Asignación de memoria: " + algoritmo.getEtiqueta() + ". Los bloques ya ocupados no se mueven.");
    }

    public Proceso crear(String nombre, int prioridad, int llegada, int rafaga, int paginas, int rafagaES, int[] maxRecursos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El proceso necesita un nombre.");
        }
        if (prioridad < 1 || prioridad > 10) {
            throw new IllegalArgumentException("La prioridad debe estar entre 1 y 10.");
        }
        if (llegada < 0) {
            throw new IllegalArgumentException("El tiempo de llegada no puede ser negativo.");
        }
        if (rafaga < 1) {
            throw new IllegalArgumentException("La ráfaga de CPU debe ser al menos 1.");
        }
        if (paginas < 1) {
            throw new IllegalArgumentException("El tamaño debe ser de al menos 1 página.");
        }
        if (paginas > memoria.getTotalPaginas()) {
            throw new IllegalArgumentException("El proceso pide " + paginas
                    + " páginas y la memoria total es " + memoria.getTotalPaginas() + ".");
        }
        if (rafagaES < 0) {
            throw new IllegalArgumentException("La ráfaga de E/S no puede ser negativa.");
        }
        if (maxRecursos == null || maxRecursos.length != 3) {
            throw new IllegalArgumentException("Debe indicar el máximo de Impresora, Disco y Puerto.");
        }
        for (int i = 0; i < 3; i++) {
            if (maxRecursos[i] < 0) {
                throw new IllegalArgumentException("Los recursos máximos no pueden ser negativos.");
            }
            if (maxRecursos[i] > recursos.getTotal(i)) {
                throw new IllegalArgumentException(GestorRecursos.NOMBRES[i] + ": el máximo pedido ("
                        + maxRecursos[i] + ") supera las instancias del sistema (" + recursos.getTotal(i) + ").");
            }
        }
        Proceso proceso = new Proceso(siguientePid++, nombre.trim(), prioridad, llegada, rafaga, paginas, rafagaES, maxRecursos);
        procesos.add(proceso);
        transiciones.add(new Transicion(getReloj(), proceso.getPid(), proceso.getNombre(),
                null, EstadoProceso.NUEVO, "creación de proceso"));
        anotar("PCB creado: " + proceso.getEtiqueta() + " " + proceso.getNombre()
                + "  prioridad " + prioridad + "  llegada " + llegada + "  ráfaga " + rafaga
                + "  páginas " + paginas + "  recursos máx. " + proceso.recursosMaxTexto());
        return proceso;
    }

    public void crearAleatorios(int cantidad) {
        if (cantidad < 1 || cantidad > 30) {
            throw new IllegalArgumentException("Genere entre 1 y 30 procesos.");
        }
        Random azar = new Random();
        String[] nombres = {"Editor", "Compilador", "Navegador", "Correo", "Antivirus", "Reproductor",
                "Terminal", "Indexador", "Respaldo", "Monitor", "Impresor", "Buscador"};
        for (int i = 0; i < cantidad; i++) {
            int topePaginas = Math.min(8, memoria.getTotalPaginas());
            int paginas = 1 + azar.nextInt(topePaginas);
            int[] maximos = new int[3];
            for (int r = 0; r < 3; r++) {
                int tope = recursos.getTotal(r);
                maximos[r] = tope == 0 ? 0 : azar.nextInt(Math.min(tope, 2) + 1);
            }
            int es = azar.nextBoolean() ? azar.nextInt(4) : 0;
            crear(nombres[azar.nextInt(nombres.length)],
                    1 + azar.nextInt(5), azar.nextInt(12), 2 + azar.nextInt(8), paginas, es, maximos);
        }
    }

    public int cargar(List<DatosProceso> datos, boolean reemplazar) {
        if (reemplazar) {
            vaciar();
        }
        int cargados = 0;
        for (DatosProceso dato : datos) {
            crear(dato.getNombre(), dato.getPrioridad(), dato.getLlegada(), dato.getRafaga(),
                    dato.getPaginas(), dato.getRafagaES(), dato.getRecursos());
            cargados++;
        }
        return cargados;
    }

    public void cargarReferencia() {
        quantum = 4;
        intervaloAging = 2;
        vaciar();
        fijarMemoria(64);
        fijarRecursos(2, 2, 2);
        cargar(CargasEjemplo.referencia(), false);
        anotar("Carga clásica lista. Compare los cinco algoritmos con quantum 4.");
    }

    public void cargarConEntradaSalida() {
        vaciar();
        fijarMemoria(64);
        fijarRecursos(2, 2, 2);
        cargar(CargasEjemplo.conEntradaSalida(), false);
        anotar("Carga con E/S: los procesos pasan a Bloqueado a mitad de su ráfaga y vuelven a Listo.");
    }

    public void cargarMemoriaYRecursos() {
        limitarMemoriaRecursos = true;
        vaciar();
        fijarMemoria(16);
        fijarRecursos(1, 1, 1);
        cargar(CargasEjemplo.memoriaYRecursos(), false);
        anotar("Escenario de 16 páginas y 1 impresora. MemB espera el recurso; MemC espera memoria hasta que MemA termine.");
    }

    public String demostrarFragmentacion() {
        vaciar();
        fijarMemoria(32);
        fijarRecursos(2, 2, 2);
        Proceso a = crear("BloqueA", 1, 0, 4, 8, 0, new int[]{0, 0, 0});
        Proceso b = crear("BloqueB", 1, 0, 4, 8, 0, new int[]{0, 0, 0});
        Proceso c = crear("BloqueC", 1, 0, 4, 8, 0, new int[]{0, 0, 0});
        Proceso d = crear("BloqueD", 1, 0, 4, 12, 0, new int[]{0, 0, 0});
        memoria.asignar(a);
        memoria.asignar(b);
        memoria.asignar(c);
        memoria.liberar(b);
        boolean cupo = memoria.asignar(d);
        int libres = memoria.paginasLibres();
        int mayor = memoria.huecoMayor();
        String informe = "Se ocuparon A, B y C (8 páginas cada uno) y se liberó B. "
                + "Quedan " + libres + " páginas libres, pero el hueco mayor es de " + mayor
                + ". D necesita 12. "
                + (cupo ? "D cupo en un hueco contiguo." : "D no cabe: hay fragmentación externa (la memoria libre no es contigua).");
        anotar(informe);
        notificar();
        return informe;
    }

    public void fijarMemoria(int paginas) {
        if (paginas < 1 || paginas > 512) {
            throw new IllegalArgumentException("Use entre 1 y 512 páginas.");
        }
        for (Proceso proceso : procesos) {
            if (proceso.getPaginas() > paginas) {
                throw new IllegalArgumentException(proceso.getEtiqueta() + " pide " + proceso.getPaginas()
                        + " páginas. Vacíe los procesos o elija un tamaño mayor.");
            }
        }
        AlgoritmoMemoria alg = memoria.getAlgoritmo();
        invalidarSimulacion();
        memoria = new GestorMemoria(paginas, alg);
        for (Proceso proceso : procesos) {
            proceso.soltarMemoria();
        }
        anotar("Memoria redimensionada a " + paginas + " páginas (" + (paginas * KB_POR_PAGINA) + " KB).");
    }

    public void fijarRecursos(int impresora, int disco, int puerto) {
        if (impresora < 0 || disco < 0 || puerto < 0) {
            throw new IllegalArgumentException("Las instancias no pueden ser negativas.");
        }
        int[] totales = {impresora, disco, puerto};
        for (Proceso proceso : procesos) {
            int[] maximos = proceso.getMaxRecursos();
            for (int i = 0; i < 3; i++) {
                if (maximos[i] > totales[i]) {
                    throw new IllegalArgumentException(proceso.getEtiqueta() + " pide " + maximos[i] + " de "
                            + GestorRecursos.NOMBRES[i] + ". Vacíe los procesos o aumente las instancias.");
                }
            }
        }
        invalidarSimulacion();
        recursos = new GestorRecursos(new int[]{impresora, disco, puerto});
        anotar("Recursos del sistema: Impresora " + impresora + ", Disco " + disco + ", Puerto " + puerto + ".");
    }

    public void vaciar() {
        invalidarSimulacion();
        procesos.clear();
        transiciones.clear();
        siguientePid = 1;
        ipc.vaciar();
        memoria.reiniciar();
        recursos.reiniciar();
        anotar("Se eliminaron todos los procesos y se liberó la memoria.");
    }

    public void terminar(Proceso proceso) {
        terminar(proceso, "terminación controlada por el usuario");
    }

    public void terminar(Proceso proceso, String causa) {
        if (proceso.getEstado() == EstadoProceso.TERMINADO) {
            return;
        }
        if (proceso.tieneMemoria()) {
            memoria.liberar(proceso);
        }
        recursos.liberar(proceso);
        ipc.retirar(proceso);
        if (motor != null) {
            motor.retirar(proceso);
        }
        proceso.setFin(getReloj());
        proceso.setFinalizoNormal(false);
        proceso.cambiarEstado(EstadoProceso.TERMINADO, causa, getReloj(), transiciones);
        anotar(proceso.getEtiqueta() + " terminado (" + causa + "). Memoria y recursos liberados.");
    }

    public void iniciarSimulacion() {
        if (procesos.isEmpty()) {
            throw new IllegalArgumentException("Cree o cargue al menos un proceso.");
        }
        for (Proceso proceso : procesos) {
            proceso.reiniciarEjecucion();
        }
        memoria.reiniciar();
        recursos.reiniciar();
        ipc.reiniciar();
        transiciones.clear();
        for (Proceso proceso : procesos) {
            transiciones.add(new Transicion(0, proceso.getPid(), proceso.getNombre(),
                    null, EstadoProceso.NUEVO, "creación de proceso"));
        }
        motor = new MotorPlanificacion(procesos, memoria, recursos, transiciones,
                algoritmo, quantum, intervaloAging, limitarMemoriaRecursos);
        vista = motor.vista();
        anotar("Simulación preparada con " + algoritmo.getEtiqueta()
                + (algoritmo == AlgoritmoCPU.ROUND_ROBIN ? " (q=" + quantum + ")" : "")
                + (algoritmo == AlgoritmoCPU.PRIORIDAD_AGING ? " (envejece cada " + intervaloAging + ")" : "")
                + ".");
    }

    public PasoSimulacion paso() {
        if (motor == null) {
            iniciarSimulacion();
        }
        PasoSimulacion resultado = motor.paso();
        vista = motor.vista();
        if (!motor.getMensaje().isBlank() && resultado == PasoSimulacion.ATASCO) {
            anotar(motor.getMensaje());
        }
        notificar();
        return resultado;
    }

    public void detenerSimulacion() {
        invalidarSimulacion();
        for (Proceso proceso : procesos) {
            proceso.reiniciarEjecucion();
        }
        memoria.reiniciar();
        recursos.reiniciar();
        anotar("Simulación reiniciada. Los PCB conservan su definición original.");
    }

    public List<ResultadoPlanificacion> comparar() {
        if (procesos.isEmpty()) {
            throw new IllegalArgumentException("No hay carga para comparar.");
        }
        List<ResultadoPlanificacion> resultados = new ArrayList<>();
        for (AlgoritmoCPU alg : AlgoritmoCPU.values()) {
            List<Proceso> copia = new ArrayList<>();
            for (Proceso proceso : procesos) {
                copia.add(proceso.copiaLimpia());
            }
            resultados.add(MotorPlanificacion.ejecutar(
                    copia, memoria.getTotalPaginas(), memoria.getAlgoritmo(),
                    new int[]{recursos.getTotal(0), recursos.getTotal(1), recursos.getTotal(2)},
                    alg, quantum, intervaloAging, limitarMemoriaRecursos));
        }
        comparacion = resultados;
        anotar("Se ejecutó la misma carga con FCFS, SJF, SRTF, Round Robin y Prioridad con envejecimiento.");
        notificar();
        return resultados;
    }

    public boolean asignarMemoria(Proceso proceso) {
        if (proceso.getEstado() == EstadoProceso.TERMINADO) {
            throw new IllegalArgumentException("El proceso ya terminó.");
        }
        if (proceso.tieneMemoria()) {
            return true;
        }
        boolean ok = memoria.asignar(proceso);
        if (ok) {
            anotar(proceso.getEtiqueta() + " recibió " + proceso.getPaginas()
                    + " páginas desde la base " + proceso.getBaseMemoria() + " (" + memoria.getAlgoritmo().getEtiqueta() + ").");
        } else {
            anotar(proceso.getEtiqueta() + " no obtuvo memoria: no hay un hueco contiguo de "
                    + proceso.getPaginas() + " páginas.");
        }
        notificar();
        return ok;
    }

    public void liberarMemoria(Proceso proceso) {
        if (!proceso.tieneMemoria()) {
            return;
        }
        memoria.liberar(proceso);
        anotar("Memoria de " + proceso.getEtiqueta() + " liberada y huecos adyacentes fusionados.");
    }

    public void crearSemaforo(String nombre, int valor) {
        ipc.crearSemaforo(nombre, valor);
        anotar("Semáforo «" + nombre.trim() + "» creado con valor " + valor + ".");
    }

    public void esperarSemaforo(Proceso proceso, String nombre) {
        exigirVivo(proceso);
        boolean adquirido = ipc.esperar(proceso, nombre);
        SemaforoSO semaforo = ipc.getSemaforo(nombre);
        if (adquirido) {
            anotar(proceso.getEtiqueta() + " hizo wait sobre «" + nombre + "». Valor actual: " + semaforo.getValor() + ".");
        } else {
            bloquearPorIpc(proceso, MotivoBloqueo.SEMAFORO, "espera de semáforo");
            anotar(proceso.getEtiqueta() + " quedó Bloqueado: «" + nombre + "» está en cero.");
        }
    }

    public void senalarSemaforo(String nombre) {
        Proceso despertado = ipc.senalar(nombre);
        if (despertado != null) {
            despertar(despertado, "señal de semáforo");
            anotar("signal sobre «" + nombre + "» despertó a " + despertado.getEtiqueta() + ".");
        } else {
            anotar("signal sobre «" + nombre + "». Valor actual: " + ipc.getSemaforo(nombre).getValor() + ".");
        }
    }

    public void enviar(Proceso emisor, Proceso receptor, String texto) {
        exigirVivo(emisor);
        exigirVivo(receptor);
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escriba el mensaje.");
        }
        Proceso dormido = ipc.enviar(emisor, receptor, texto.trim(), getReloj());
        if (dormido != null) {
            despertar(dormido, "mensaje recibido");
            anotar(emisor.getEtiqueta() + " entregó «" + texto.trim() + "» a " + receptor.getEtiqueta() + ", que esperaba recibirlo.");
        } else {
            anotar(emisor.getEtiqueta() + " dejó «" + texto.trim() + "» en el buzón de " + receptor.getEtiqueta() + ".");
        }
    }

    public void recibir(Proceso receptor) {
        exigirVivo(receptor);
        Mensaje mensaje = ipc.recibir(receptor);
        if (mensaje != null) {
            anotar(receptor.getEtiqueta() + " recibió: «" + mensaje.getTexto() + "».");
        } else {
            bloquearPorIpc(receptor, MotivoBloqueo.MENSAJE, "espera de mensaje");
            anotar(receptor.getEtiqueta() + " quedó Bloqueado a la espera de un mensaje.");
        }
    }

    public void demostracionIpc(Proceso a, Proceso b) {
        exigirVivo(a);
        exigirVivo(b);
        prepararDemostracion(a);
        prepararDemostracion(b);
        if (!existeSemaforo("mutex")) {
            ipc.crearSemaforo("mutex", 1);
            anotar("Semáforo «mutex» creado con valor 1.");
        } else {
            ipc.getSemaforo("mutex").reiniciar();
        }
        ipc.limpiarBandeja(b.getPid());
        esperarSemaforo(a, "mutex");
        esperarSemaforo(b, "mutex");
        senalarSemaforo("mutex");
        recibir(b);
        enviar(a, b, "dato listo");
        anotar("Demostración IPC terminada: B esperó el mutex, fue despertado con signal y luego recibió el mensaje de A.");
    }

    public String ejecutarDivision(Proceso proceso, int dividendo, int divisor, boolean recuperar) {
        exigirOperable(proceso);
        ManejadorExcepciones.Resultado resultado = manejador.dividir(proceso, dividendo, divisor, getReloj(), recuperar);
        aplicarManejo(proceso, resultado, "excepción: división por cero");
        return resultado.detalle;
    }

    public String ejecutarAcceso(Proceso proceso, int pagina, boolean recuperar) {
        exigirOperable(proceso);
        ManejadorExcepciones.Resultado resultado = manejador.accederMemoria(memoria, proceso, pagina, getReloj(), recuperar);
        aplicarManejo(proceso, resultado, "excepción: acceso a memoria no válida");
        return resultado.detalle;
    }

    public Proceso procesoEnEjecucion() {
        if (motor != null && motor.getEnCpu() != null && motor.getEnCpu().getEstado() == EstadoProceso.EJECUCION) {
            return motor.getEnCpu();
        }
        for (Proceso proceso : procesos) {
            if (proceso.getEstado() == EstadoProceso.EJECUCION) {
                return proceso;
            }
        }
        return null;
    }

    public int contar(EstadoProceso estado) {
        int n = 0;
        for (Proceso proceso : procesos) {
            if (proceso.getEstado() == estado) n++;
        }
        return n;
    }

    private void aplicarManejo(Proceso proceso, ManejadorExcepciones.Resultado resultado, String causaFatal) {
        if (resultado.registro != null) {
            excepciones.add(resultado.registro);
        }
        if (resultado.terminarProceso) {
            terminar(proceso, causaFatal);
        } else {
            anotar(proceso.getEtiqueta() + ": " + resultado.detalle);
        }
    }

    private void bloquearPorIpc(Proceso proceso, MotivoBloqueo motivo, String causa) {
        if (motor != null) {
            motor.retirar(proceso);
        }
        proceso.setMotivo(motivo);
        proceso.cambiarEstado(EstadoProceso.BLOQUEADO, causa, getReloj(), transiciones);
        notificar();
    }

    private void despertar(Proceso proceso, String causa) {
        if (proceso.getEstado() != EstadoProceso.BLOQUEADO) {
            notificar();
            return;
        }
        proceso.cambiarEstado(EstadoProceso.LISTO, causa, getReloj(), transiciones);
        notificar();
    }

    private void prepararDemostracion(Proceso proceso) {
        if (proceso.getEstado() == EstadoProceso.BLOQUEADO
                && (proceso.getMotivo() == MotivoBloqueo.SEMAFORO || proceso.getMotivo() == MotivoBloqueo.MENSAJE)) {
            ipc.retirar(proceso);
            if (motor != null) {
                motor.retirar(proceso);
            }
            proceso.cambiarEstado(EstadoProceso.LISTO, "reinicio de la demostración IPC", getReloj(), transiciones);
        }
    }

    private void exigirVivo(Proceso proceso) {
        if (proceso.getEstado() == EstadoProceso.TERMINADO) {
            throw new IllegalArgumentException(proceso.getEtiqueta() + " ya terminó.");
        }
    }

    private void exigirOperable(Proceso proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("Seleccione un proceso.");
        }
    }

    private boolean existeSemaforo(String nombre) {
        for (SemaforoSO semaforo : ipc.getSemaforos()) {
            if (semaforo.getNombre().equals(nombre)) return true;
        }
        return false;
    }

    private void invalidarSimulacion() {
        motor = null;
        vista = null;
    }
}
