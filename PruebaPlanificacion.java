

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Comprueba métricas de planificación, memoria, IPC, excepciones y la carrera de hilos. */
public final class PruebaPlanificacion {
    private static int fallos;

    public static void main(String[] args) throws Exception {
        planificacionClasica();
        envejecimiento();
        entradaSalida();
        memoriaYRecursos();
        algoritmosDeMemoria();
        accesoInvalidoYDivision();
        archivos();
        ipc();
        carrera();
        if (fallos > 0) {
            System.err.println("RESULTADO: " + fallos + " comprobaciones fallaron.");
            System.exit(1);
        }
        System.out.println("RESULTADO: todas las comprobaciones pasaron.");
    }

    private static void planificacionClasica() {
        ResultadoPlanificacion fcfs = correr(AlgoritmoCPU.FCFS, 4, 2);
        verificar("P1:0-8;P2:8-12;P3:12-21;P4:21-26;".equals(fcfs.firmaGantt()),
                "FCFS gantt esperado P1:0-8;P2:8-12;P3:12-21;P4:21-26; obtenido " + fcfs.firmaGantt());
        cerca(fcfs.getEsperaPromedio(), 8.75, "FCFS espera");
        cerca(fcfs.getRetornoPromedio(), 15.25, "FCFS retorno");
        cerca(fcfs.getRespuestaPromedio(), 8.75, "FCFS respuesta");
        cerca(fcfs.getUtilizacionCpu(), 100, "FCFS utilización");
        cerca(fcfs.getThroughput(), 4.0 / 26, "FCFS throughput");
        invariante(fcfs);

        ResultadoPlanificacion sjf = correr(AlgoritmoCPU.SJF, 4, 2);
        verificar("P1:0-8;P2:8-12;P4:12-17;P3:17-26;".equals(sjf.firmaGantt()),
                "SJF gantt obtenido " + sjf.firmaGantt());
        cerca(sjf.getEsperaPromedio(), 7.75, "SJF espera");
        invariante(sjf);

        ResultadoPlanificacion srtf = correr(AlgoritmoCPU.SRTF, 4, 2);
        verificar("P1:0-1;P2:1-5;P4:5-10;P1:10-17;P3:17-26;".equals(srtf.firmaGantt()),
                "SRTF gantt obtenido " + srtf.firmaGantt());
        cerca(srtf.getEsperaPromedio(), 6.5, "SRTF espera");
        cerca(srtf.getRespuestaPromedio(), 4.25, "SRTF respuesta");
        invariante(srtf);
        verificar(contiene(srtf, "expropiación: menor tiempo restante (SRTF)"), "SRTF registra la expropiación");

        ResultadoPlanificacion rr = correr(AlgoritmoCPU.ROUND_ROBIN, 4, 2);
        verificar("P1:0-4;P2:4-8;P3:8-12;P4:12-16;P1:16-20;P3:20-24;P4:24-25;P3:25-26;"
                        .equals(rr.firmaGantt()),
                "RR gantt obtenido " + rr.firmaGantt());
        cerca(rr.getEsperaPromedio(), 11.75, "RR espera");
        cerca(rr.getRetornoPromedio(), 18.25, "RR retorno");
        cerca(rr.getRespuestaPromedio(), 4.5, "RR respuesta");
        invariante(rr);
        verificar(contiene(rr, "fin de quantum"), "RR registra fin de quantum");
    }

    private static void envejecimiento() {
        ResultadoPlanificacion sin = envejecimiento(1000);
        ResultadoPlanificacion con = envejecimiento(2);
        Proceso bajoSin = sin.getProcesos().get(0);
        Proceso bajoCon = con.getProcesos().get(0);
        verificar(bajoSin.getInicioCpu() == 20, "Sin envejecimiento L entra en t=20 y entró en " + bajoSin.getInicioCpu());
        verificar(bajoCon.getInicioCpu() == 10, "Con envejecimiento L entra en t=10 y entró en " + bajoCon.getInicioCpu());
        verificar(contiene(con, "envejecimiento:"), "Se registra el aumento de prioridad");
        verificar(contiene(con, "expropiación: mayor prioridad"), "La prioridad envejecida expropia al proceso largo");
        invariante(sin);
        invariante(con);
    }

    private static ResultadoPlanificacion envejecimiento(int intervalo) {
        List<Proceso> procesos = List.of(
                new Proceso(1, "L", 1, 0, 4, 2, 0, new int[]{0, 0, 0}),
                new Proceso(2, "H", 5, 0, 20, 2, 0, new int[]{0, 0, 0})
        );
        return MotorPlanificacion.ejecutar(new ArrayList<>(procesos), 32, AlgoritmoMemoria.FIRST_FIT,
                new int[]{1, 1, 1}, AlgoritmoCPU.PRIORIDAD_AGING, 4, intervalo, true);
    }

    private static void entradaSalida() {
        List<Proceso> procesos = new ArrayList<>();
        procesos.add(new Proceso(1, "Editor", 1, 0, 4, 2, 2, new int[]{0, 0, 0}));
        ResultadoPlanificacion r = MotorPlanificacion.ejecutar(procesos, 32, AlgoritmoMemoria.FIRST_FIT,
                new int[]{1, 1, 1}, AlgoritmoCPU.FCFS, 4, 2, true);
        verificar("Editor:0-2;Editor:4-6;".equals(r.firmaGantt()), "E/S gantt obtenido " + r.firmaGantt());
        verificar(contiene(r, "E/S") && contiene(r, "fin de E/S"), "Transiciones de E/S");
        invariante(r);
    }

    private static void memoriaYRecursos() {
        List<Proceso> procesos = new ArrayList<>();
        procesos.add(new Proceso(1, "MemA", 1, 0, 3, 8, 0, new int[]{1, 0, 0}));
        procesos.add(new Proceso(2, "MemB", 1, 0, 3, 8, 0, new int[]{1, 0, 0}));
        procesos.add(new Proceso(3, "MemC", 1, 0, 3, 10, 0, new int[]{0, 0, 0}));
        ResultadoPlanificacion r = MotorPlanificacion.ejecutar(procesos, 16, AlgoritmoMemoria.FIRST_FIT,
                new int[]{1, 1, 1}, AlgoritmoCPU.FCFS, 4, 2, true);
        verificar(contiene(r, "espera de recurso"), "MemB espera la impresora");
        verificar(contiene(r, "espera de memoria"), "MemC espera páginas contiguas");
        verificar(r.getCompletados() == 3, "Los tres procesos terminan cuando se libera memoria y el recurso");
        invariante(r);
        for (Proceso p : r.getProcesos()) {
            verificar(!p.tieneMemoria(), p.getNombre() + " liberó su memoria al terminar");
        }
    }

    private static void algoritmosDeMemoria() {
        int[] bases = new int[3];
        AlgoritmoMemoria[] algs = {AlgoritmoMemoria.FIRST_FIT, AlgoritmoMemoria.BEST_FIT, AlgoritmoMemoria.WORST_FIT};
        for (int i = 0; i < algs.length; i++) {
            GestorMemoria memoria = new GestorMemoria(30, algs[i]);
            Proceso a = new Proceso(1, "A", 1, 0, 1, 10, 0, new int[]{0, 0, 0});
            Proceso b = new Proceso(2, "B", 1, 0, 1, 6, 0, new int[]{0, 0, 0});
            Proceso c = new Proceso(3, "C", 1, 0, 1, 10, 0, new int[]{0, 0, 0});
            Proceso d = new Proceso(4, "D", 1, 0, 1, 4, 0, new int[]{0, 0, 0});
            memoria.asignar(a);
            memoria.asignar(b);
            memoria.asignar(c);
            memoria.liberar(b);
            verificar(memoria.asignar(d), algs[i].getEtiqueta() + " asigna D");
            bases[i] = d.getBaseMemoria();
            verificar(memoria.fragmentacionExterna() >= 0, "Fragmentación definida");
        }
        verificar(bases[0] == 10, "First Fit usa el primer hueco (base 10), obtuvo " + bases[0]);
        verificar(bases[1] == 26, "Best Fit usa el hueco de 4 (base 26), obtuvo " + bases[1]);
        verificar(bases[2] == 10, "Worst Fit usa el hueco de 6 (base 10), obtuvo " + bases[2]);
    }

    private static void accesoInvalidoYDivision() {
        ManejadorExcepciones manejador = new ManejadorExcepciones();
        Proceso proceso = new Proceso(1, "Calc", 1, 0, 3, 4, 0, new int[]{0, 0, 0});
        GestorMemoria memoria = new GestorMemoria(16, AlgoritmoMemoria.FIRST_FIT);
        memoria.asignar(proceso);
        ManejadorExcepciones.Resultado ok = manejador.accederMemoria(memoria, proceso, 1, 0, false);
        verificar(ok.exito && ok.valor == 1, "Traducción lógica a física");
        ManejadorExcepciones.Resultado malo = manejador.accederMemoria(memoria, proceso, 9, 0, false);
        verificar(!malo.exito && malo.terminarProceso, "La página 9 se rechaza y el proceso debe terminar");
        verificar(malo.registro.getTipo().contains("memoria"), "El registro identifica el acceso inválido");
        ManejadorExcepciones.Resultado division = manejador.dividir(proceso, 8, 0, 3, true);
        verificar(!division.exito && !division.terminarProceso, "División por cero recuperable no termina el proceso");
        ManejadorExcepciones.Resultado fatal = manejador.dividir(proceso, 8, 0, 3, false);
        verificar(fatal.terminarProceso, "División por cero fatal termina el proceso");
        boolean lanzo = false;
        try {
            memoria.liberar(proceso);
            memoria.acceder(proceso, 0);
        } catch (AccesoMemoriaInvalidoException ex) {
            lanzo = true;
        }
        verificar(lanzo, "Acceder tras liberar la memoria lanza la excepción");
    }

    private static void archivos() throws Exception {
        String json = """
                { "procesos": [
                  {"nombre":"Editor","prioridad":3,"llegada":0,"rafaga":6,"paginas":4,"es":2,"recursos":[1, 0, 0]}
                ]}
                """;
        List<DatosProceso> desdeJson = CargadorProcesos.desdeJson(json);
        verificar(desdeJson.size() == 1 && desdeJson.get(0).getRafaga() == 6, "JSON de un proceso");
        verificar(desdeJson.get(0).getRecursos()[0] == 1, "JSON lee el vector de recursos");
        String csv = "nombre,prioridad,llegada,rafaga,paginas,es,r1,r2,r3\nP1,2,0,8,2,0,0,0,0\n";
        List<DatosProceso> desdeCsv = CargadorProcesos.desdeCsv(csv);
        verificar(desdeCsv.size() == 1 && desdeCsv.get(0).getNombre().equals("P1"), "CSV de un proceso");
        List<DatosProceso> archivo = CargadorProcesos.leer(Path.of("carga_referencia.json"));
        verificar(archivo.size() == 4, "El archivo de referencia trae 4 procesos");
        Sistema sistema = new Sistema();
        sistema.cargarReferencia();
        List<ResultadoPlanificacion> comparacion = sistema.comparar();
        verificar(comparacion.size() == 5, "La misma carga se ejecuta con los cinco algoritmos");
        verificar("P1:0-8;P2:8-12;P3:12-21;P4:21-26;".equals(comparacion.get(0).firmaGantt()),
                "La comparación FCFS coincide con el ejemplo");
    }

    private static void ipc() {
        GestorIPC ipc = new GestorIPC();
        ipc.crearSemaforo("mutex", 1);
        Proceso a = new Proceso(1, "A", 1, 0, 1, 1, 0, new int[]{0, 0, 0});
        Proceso b = new Proceso(2, "B", 1, 0, 1, 1, 0, new int[]{0, 0, 0});
        verificar(ipc.esperar(a, "mutex"), "A adquiere el mutex");
        verificar(!ipc.esperar(b, "mutex"), "B se bloquea en el mutex");
        verificar(ipc.senalar("mutex") == b, "signal despierta a B y no a otro");
        verificar(ipc.recibir(b) == null, "B se bloquea si el buzón está vacío");
        verificar(ipc.enviar(a, b, "hola", 1) == b, "El mensaje despierta al receptor");
        verificar("hola".equals(b.getUltimoMensaje()), "El PCB guarda el mensaje recibido");
    }

    private static void carrera() throws InterruptedException {
        DemoProductorConsumidor.Informe inseguro = null;
        for (int intento = 0; intento < 4 && (inseguro == null || inseguro.anomalias == 0); intento++) {
            inseguro = DemoProductorConsumidor.contador(false, 4, 20);
        }
        DemoProductorConsumidor.Informe seguro = DemoProductorConsumidor.contador(true, 4, 20);
        verificar(inseguro.obtenido < inseguro.esperado, "Sin lock se pierden incrementos ("
                + inseguro.obtenido + " de " + inseguro.esperado + ")");
        verificar(seguro.obtenido == seguro.esperado, "Con ReentrantLock el contador es exacto");

        DemoProductorConsumidor.Informe bufferSeguro = DemoProductorConsumidor.buffer(true, 4, 2, 2, 8, null);
        verificar(bufferSeguro.anomalias == 0 && bufferSeguro.producidos == bufferSeguro.consumidos,
                "Buffer con Semaphore y ReentrantLock consistente");

        boolean vioCarrera = false;
        for (int intento = 0; intento < 4 && !vioCarrera; intento++) {
            DemoProductorConsumidor.Informe buffer = DemoProductorConsumidor.buffer(false, 4, 2, 2, 6, null);
            vioCarrera = buffer.anomalias > 0;
        }
        verificar(vioCarrera, "El buffer sin sincronización muestra la condición de carrera");
    }

    private static ResultadoPlanificacion correr(AlgoritmoCPU algoritmo, int quantum, int aging) {
        List<Proceso> procesos = new ArrayList<>();
        procesos.add(new Proceso(1, "P1", 2, 0, 8, 2, 0, new int[]{0, 0, 0}));
        procesos.add(new Proceso(2, "P2", 3, 1, 4, 2, 0, new int[]{0, 0, 0}));
        procesos.add(new Proceso(3, "P3", 1, 2, 9, 2, 0, new int[]{0, 0, 0}));
        procesos.add(new Proceso(4, "P4", 4, 3, 5, 2, 0, new int[]{0, 0, 0}));
        return MotorPlanificacion.ejecutar(procesos, 64, AlgoritmoMemoria.FIRST_FIT,
                new int[]{2, 2, 2}, algoritmo, quantum, aging, true);
    }

    private static void invariante(ResultadoPlanificacion resultado) {
        for (Proceso p : resultado.getProcesos()) {
            if (!p.isFinalizoNormal()) {
                continue;
            }
            int suma = p.getTiempoEspera() + p.getRafaga() + p.getTiempoBloqueado();
            verificar(p.getRetorno() == suma, p.getNombre() + " en " + resultado.getAlgoritmo().getEtiqueta()
                    + ": retorno " + p.getRetorno() + " distinto de espera+ráfaga+bloqueo=" + suma);
            verificar(p.getEstado() == EstadoProceso.TERMINADO, p.getNombre() + " terminó");
        }
    }

    private static boolean contiene(ResultadoPlanificacion resultado, String texto) {
        for (Transicion transicion : resultado.getTransiciones()) {
            if (transicion.getCausa() != null && transicion.getCausa().contains(texto)) {
                return true;
            }
        }
        return false;
    }

    private static void cerca(double valor, double esperado, String etiqueta) {
        verificar(Math.abs(valor - esperado) < 0.001, etiqueta + " esperado " + esperado + " obtenido " + valor);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            fallos++;
            System.err.println("FALLO: " + mensaje);
        }
    }
}
