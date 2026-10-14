

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

/** Mapa de páginas: cada celda es una página física. */
final class MapaMemoria extends JPanel {
    private GestorMemoria memoria;
    private static final int CELDA = 26;
    private static final int COLUMNAS = 16;

    MapaMemoria() {
        setBackground(Color.WHITE);
        setToolTipText("");
    }

    void setMemoria(GestorMemoria memoria) {
        this.memoria = memoria;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int total = memoria == null ? 64 : memoria.getTotalPaginas();
        int filas = Math.max(1, (total + COLUMNAS - 1) / COLUMNAS);
        return new Dimension(COLUMNAS * CELDA + 2, filas * CELDA + 2);
    }

    @Override
    public String getToolTipText(MouseEvent evento) {
        if (memoria == null) return null;
        int indice = indice(evento.getX(), evento.getY());
        if (indice < 0 || indice >= memoria.getTotalPaginas()) return null;
        for (BloqueMemoria bloque : memoria.getBloques()) {
            if (indice >= bloque.getInicio() && indice < bloque.getFin()) {
                if (bloque.estaLibre()) {
                    return "Página " + indice + ": libre";
                }
                return "Página " + indice + ": " + bloque.getNombre() + " (P" + bloque.getPid() + ")";
            }
        }
        return "Página " + indice;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (memoria == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Estilos.MONO.deriveFont(10f));
        int total = memoria.getTotalPaginas();
        for (int i = 0; i < total; i++) {
            int x = (i % COLUMNAS) * CELDA;
            int y = (i / COLUMNAS) * CELDA;
            g2.setColor(color(i));
            g2.fillRect(x, y, CELDA - 2, CELDA - 2);
            g2.setColor(new Color(148, 163, 184));
            g2.drawRect(x, y, CELDA - 2, CELDA - 2);
        }
    }

    private Color color(int pagina) {
        for (BloqueMemoria bloque : memoria.getBloques()) {
            if (pagina >= bloque.getInicio() && pagina < bloque.getFin()) {
                return bloque.estaLibre() ? Colores.LIBRE : Colores.dePid(bloque.getPid());
            }
        }
        return Colores.LIBRE;
    }

    private int indice(int x, int y) {
        if (x < 0 || y < 0) return -1;
        int columna = x / CELDA;
        int fila = y / CELDA;
        if (columna >= COLUMNAS) return -1;
        return fila * COLUMNAS + columna;
    }
}
