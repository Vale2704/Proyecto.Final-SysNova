

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Memoria física contigua medida en páginas.
 * First Fit, Best Fit y Worst Fit. Al liberar se fusionan huecos adyacentes.
 */
public final class GestorMemoria {
    private final int totalPaginas;
    private AlgoritmoMemoria algoritmo;
    private final List<BloqueMemoria> bloques = new ArrayList<>();

    public GestorMemoria(int totalPaginas, AlgoritmoMemoria algoritmo) {
        if (totalPaginas < 1) {
            throw new IllegalArgumentException("La memoria debe tener al menos una página.");
        }
        this.totalPaginas = totalPaginas;
        this.algoritmo = algoritmo == null ? AlgoritmoMemoria.FIRST_FIT : algoritmo;
        reiniciar();
    }

    public void reiniciar() {
        bloques.clear();
        bloques.add(new BloqueMemoria(0, totalPaginas, -1, ""));
    }

    public void setAlgoritmo(AlgoritmoMemoria algoritmo) {
        this.algoritmo = algoritmo;
    }

    public AlgoritmoMemoria getAlgoritmo() { return algoritmo; }
    public int getTotalPaginas() { return totalPaginas; }

    public boolean cabe(Proceso proceso) {
        return buscarHueco(proceso.getPaginas()) != null;
    }

    public boolean asignar(Proceso proceso) {
        if (proceso.tieneMemoria()) {
            return true;
        }
        int need = proceso.getPaginas();
        int indice = indiceHueco(need);
        if (indice < 0) {
            return false;
        }
        BloqueMemoria hueco = bloques.get(indice);
        int inicio = hueco.getInicio();
        if (hueco.getTamano() == need) {
            hueco.ocupar(proceso.getPid(), proceso.getNombre());
        } else {
            hueco.setInicio(inicio + need);
            hueco.setTamano(hueco.getTamano() - need);
            bloques.add(indice, new BloqueMemoria(inicio, need, proceso.getPid(), proceso.getNombre()));
        }
        proceso.asignarMemoria(inicio);
        return true;
    }

    public void liberar(Proceso proceso) {
        if (!proceso.tieneMemoria()) {
            return;
        }
        for (BloqueMemoria bloque : bloques) {
            if (bloque.getPid() == proceso.getPid()) {
                bloque.liberar();
                break;
            }
        }
        fusionar();
        proceso.soltarMemoria();
    }

    public void liberarTodo() {
        reiniciar();
    }

    /**
     * Traduce una página lógica a física.
     * Lanza AccesoMemoriaInvalidoException si la página no pertenece al proceso
     * o si el proceso no tiene marcos asignados.
     */
    public int acceder(Proceso proceso, int paginaLogica) {
        if (paginaLogica < 0 || paginaLogica >= proceso.getPaginas()) {
            throw new AccesoMemoriaInvalidoException(
                    "Acceso a memoria no válida: la página " + paginaLogica
                            + " está fuera del espacio de " + proceso.getEtiqueta()
                            + " (páginas válidas: 0 a " + (proceso.getPaginas() - 1) + ").");
        }
        if (!proceso.tieneMemoria()) {
            throw new AccesoMemoriaInvalidoException(
                    "Acceso a memoria no válida: " + proceso.getEtiqueta()
                            + " no tiene marcos asignados.");
        }
        return proceso.getBaseMemoria() + paginaLogica;
    }

    public int paginasUsadas() {
        int n = 0;
        for (BloqueMemoria b : bloques) {
            if (!b.estaLibre()) n += b.getTamano();
        }
        return n;
    }

    public int paginasLibres() {
        return totalPaginas - paginasUsadas();
    }

    public int numeroHuecos() {
        int n = 0;
        for (BloqueMemoria b : bloques) {
            if (b.estaLibre()) n++;
        }
        return n;
    }

    public int huecoMayor() {
        int mayor = 0;
        for (BloqueMemoria b : bloques) {
            if (b.estaLibre() && b.getTamano() > mayor) mayor = b.getTamano();
        }
        return mayor;
    }

    /** Memoria libre que no cabe en el hueco más grande: fragmentación externa. */
    public int fragmentacionExterna() {
        int libres = paginasLibres();
        int mayor = huecoMayor();
        return Math.max(0, libres - mayor);
    }

    public List<BloqueMemoria> getBloques() {
        return Collections.unmodifiableList(bloques);
    }

    private BloqueMemoria buscarHueco(int need) {
        int indice = indiceHueco(need);
        return indice < 0 ? null : bloques.get(indice);
    }

    private int indiceHueco(int need) {
        int elegido = -1;
        for (int i = 0; i < bloques.size(); i++) {
            BloqueMemoria b = bloques.get(i);
            if (!b.estaLibre() || b.getTamano() < need) {
                continue;
            }
            if (elegido < 0) {
                elegido = i;
                if (algoritmo == AlgoritmoMemoria.FIRST_FIT) {
                    return elegido;
                }
                continue;
            }
            int tamElegido = bloques.get(elegido).getTamano();
            if (algoritmo == AlgoritmoMemoria.BEST_FIT && b.getTamano() < tamElegido) {
                elegido = i;
            } else if (algoritmo == AlgoritmoMemoria.WORST_FIT && b.getTamano() > tamElegido) {
                elegido = i;
            }
        }
        return elegido;
    }

    private void fusionar() {
        for (int i = 0; i < bloques.size() - 1; ) {
            BloqueMemoria actual = bloques.get(i);
            BloqueMemoria siguiente = bloques.get(i + 1);
            if (actual.estaLibre() && siguiente.estaLibre()) {
                actual.setTamano(actual.getTamano() + siguiente.getTamano());
                bloques.remove(i + 1);
            } else {
                i++;
            }
        }
    }
}
