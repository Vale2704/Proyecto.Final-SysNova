
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Problema clásico productor–consumidor y una carrera sobre un contador compartido.
 * Los hilos son java.lang.Thread. La versión correcta usa Semaphore y ReentrantLock.
 */
public final class DemoProductorConsumidor {
    private DemoProductorConsumidor() {}

    public static final class Informe {
        public final boolean sincronizado;
        public final String titulo;
        public final int esperado;
        public final int obtenido;
        public final int producidos;
        public final int consumidos;
        public final int anomalias;
        public final int[] bufferFinal;
        public final List<String> eventos;
        public final String conclusion;

        Informe(boolean sincronizado, String titulo, int esperado, int obtenido,
                int producidos, int consumidos, int anomalias, int[] bufferFinal,
                List<String> eventos, String conclusion) {
            this.sincronizado = sincronizado;
            this.titulo = titulo;
            this.esperado = esperado;
            this.obtenido = obtenido;
            this.producidos = producidos;
            this.consumidos = consumidos;
            this.anomalias = anomalias;
            this.bufferFinal = bufferFinal;
            this.eventos = eventos;
            this.conclusion = conclusion;
        }
    }

    public static Informe contador(boolean sincronizado, int hilos, int porHilo) throws InterruptedException {
        Contador contador = sincronizado ? new ContadorSeguro() : new ContadorInseguro();
        Thread[] trabajadores = new Thread[hilos];
        for (int i = 0; i < hilos; i++) {
            int id = i + 1;
            trabajadores[i] = new Thread(() -> {
                for (int n = 0; n < porHilo; n++) {
                    try {
                        contador.incrementar();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, (sincronizado ? "seguro-" : "carrera-") + id);
            trabajadores[i].start();
        }
        for (Thread trabajador : trabajadores) {
            trabajador.join();
        }
        int esperado = hilos * porHilo;
        int obtenido = contador.leer();
        int perdidas = Math.max(0, esperado - obtenido);
        String conclusion = sincronizado
                ? "Con ReentrantLock el contador coincide con el valor esperado. No hubo actualizaciones perdidas."
                : "Sin exclusión mutua se perdieron " + perdidas
                + " incrementos: varios hilos leyeron el mismo valor y se pisaron al escribir.";
        return new Informe(sincronizado, "Contador compartido", esperado, obtenido,
                esperado, obtenido, perdidas, new int[0], List.of(), conclusion);
    }

    public static Informe buffer(boolean sincronizado, int capacidad, int productores,
                                 int consumidores, int porProductor, Consumer<int[]> alCambiar)
            throws InterruptedException {
        Buffer buffer = new Buffer(capacidad, sincronizado, alCambiar);
        int total = productores * porProductor;
        Thread[] hilos = new Thread[productores + consumidores];
        int base = 1;
        for (int i = 0; i < productores; i++) {
            int desde = base;
            int cantidad = porProductor;
            base += porProductor;
            int id = i + 1;
            hilos[i] = new Thread(() -> {
                for (int n = 0; n < cantidad; n++) {
                    try {
                        buffer.producir(desde + n);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "productor-" + id);
            hilos[i].start();
        }
        int porConsumidor = total / consumidores;
        int resto = total % consumidores;
        for (int i = 0; i < consumidores; i++) {
            int cantidad = porConsumidor + (i < resto ? 1 : 0);
            int id = i + 1;
            hilos[productores + i] = new Thread(() -> {
                for (int n = 0; n < cantidad; n++) {
                    try {
                        buffer.consumir();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "consumidor-" + id);
            hilos[productores + i].start();
        }
        for (Thread hilo : hilos) {
            hilo.join();
        }
        int anomalias = buffer.anomalias
                + (buffer.producidos != buffer.consumidos ? 1 : 0)
                + (buffer.count != 0 ? 1 : 0)
                + (buffer.producidos != total ? 1 : 0);
        String conclusion;
        if (sincronizado) {
            conclusion = anomalias == 0
                    ? "Con Semaphore (vacíos y llenos) y ReentrantLock el buffer terminó consistente: producido = consumido."
                    : "La versión sincronizada no debió fallar. Anomalías: " + anomalias + ".";
        } else if (anomalias > 0) {
            conclusion = "Sin sincronización apareció la condición de carrera: el contador del buffer, "
                    + "las casillas o el total producido y consumido dejaron de coincidir.";
        } else {
            conclusion = "En esta ejecución el azar no mostró la carrera. Vuelva a ejecutarla: la sección crítica no está protegida.";
        }
        return new Informe(sincronizado, "Productor–Consumidor", total, buffer.consumidos,
                buffer.producidos, buffer.consumidos, anomalias, buffer.copia(),
                buffer.eventos, conclusion);
    }

    private abstract static class Contador {
        int valor;
        abstract void incrementar() throws InterruptedException;
        int leer() { return valor; }
    }

    private static final class ContadorInseguro extends Contador {
        @Override
        void incrementar() throws InterruptedException {
            int temporal = valor;
            Thread.sleep(1);
            valor = temporal + 1;
        }
    }

    private static final class ContadorSeguro extends Contador {
        private final ReentrantLock mutex = new ReentrantLock();

        @Override
        void incrementar() throws InterruptedException {
            mutex.lock();
            try {
                int temporal = valor;
                Thread.sleep(1);
                valor = temporal + 1;
            } finally {
                mutex.unlock();
            }
        }
    }

    private static final class Buffer {
        final int[] datos;
        final boolean seguro;
        final ReentrantLock mutex = new ReentrantLock();
        final Semaphore vacios;
        final Semaphore llenos;
        final Consumer<int[]> alCambiar;
        final List<String> eventos = new ArrayList<>();
        int in;
        int out;
        int count;
        int producidos;
        int consumidos;
        int anomalias;

        Buffer(int capacidad, boolean seguro, Consumer<int[]> alCambiar) {
            this.datos = new int[capacidad];
            this.seguro = seguro;
            this.vacios = new Semaphore(capacidad);
            this.llenos = new Semaphore(0);
            this.alCambiar = alCambiar;
        }

        void producir(int item) throws InterruptedException {
            if (seguro) {
                vacios.acquire();
                mutex.lock();
                try {
                    datos[in] = item;
                    in = (in + 1) % datos.length;
                    count++;
                    producidos++;
                    anotar("produce " + item + "  ocupadas=" + count);
                    publicar();
                } finally {
                    mutex.unlock();
                }
                llenos.release();
                return;
            }
            long limite = System.currentTimeMillis() + 800;
            while (System.currentTimeMillis() < limite) {
                if (count < datos.length) {
                    Thread.sleep(1);
                    int indice = in;
                    int visto = count;
                    Thread.sleep(1);
                    if (datos[indice] != 0) {
                        anomalias++;
                        anotar("pisa la casilla " + indice + " que aún tenía " + datos[indice]);
                    }
                    datos[indice] = item;
                    in = (indice + 1) % datos.length;
                    count = visto + 1;
                    producidos++;
                    if (count < 0 || count > datos.length) {
                        anomalias++;
                        anotar("contador imposible: " + count);
                    }
                    anotar("produce " + item + "  contador=" + count);
                    publicar();
                    return;
                }
                Thread.yield();
            }
            anomalias++;
            anotar("productor agotó el tiempo: el contador quedó inconsistente");
        }

        int consumir() throws InterruptedException {
            if (seguro) {
                llenos.acquire();
                mutex.lock();
                int item;
                try {
                    item = datos[out];
                    datos[out] = 0;
                    out = (out + 1) % datos.length;
                    count--;
                    consumidos++;
                    anotar("consume " + item + "  ocupadas=" + count);
                    publicar();
                } finally {
                    mutex.unlock();
                }
                vacios.release();
                return item;
            }
            long limite = System.currentTimeMillis() + 800;
            while (System.currentTimeMillis() < limite) {
                if (count > 0) {
                    Thread.sleep(1);
                    int indice = out;
                    int visto = count;
                    Thread.sleep(1);
                    int item = datos[indice];
                    datos[indice] = 0;
                    out = (indice + 1) % datos.length;
                    count = visto - 1;
                    consumidos++;
                    if (item == 0 || count < 0 || count > datos.length) {
                        anomalias++;
                        anotar("consume vacío o contador imposible (" + count + ")");
                    } else {
                        anotar("consume " + item + "  contador=" + count);
                    }
                    publicar();
                    return item;
                }
                Thread.yield();
            }
            anomalias++;
            anotar("consumidor agotó el tiempo: el contador quedó inconsistente");
            return 0;
        }

        void anotar(String texto) {
            synchronized (eventos) {
                if (eventos.size() < 80) {
                    eventos.add(Thread.currentThread().getName() + ": " + texto);
                }
            }
        }

        void publicar() {
            if (alCambiar != null) {
                alCambiar.accept(copia());
            }
        }

        int[] copia() {
            return datos.clone();
        }
    }
}
