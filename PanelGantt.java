

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Diagrama de Gantt de una ejecución de CPU. */
final class PanelGantt extends JPanel {
    private ResultadoPlanificacion resultado;

    PanelGantt() {
        setBackground(Color.WHITE);
        setToolTipText("");
    }

    void setResultado(ResultadoPlanificacion resultado) {
        this.resultado = resultado;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int tiempo = resultado == null ? 20 : Math.max(20, resultado.getTiempoTotal());
        return new Dimension(70 + tiempo * ancho(), 168);
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent evento) {
        EntradaGantt entrada = en(evento.getX());
        if (entrada == null) {
            return null;
        }
        String quien = entrada.getPid() == 0 ? "CPU ociosa" : entrada.getNombre() + " (P" + entrada.getPid() + ")";
        return quien + "  [" + entrada.getInicio() + ", " + entrada.getFin() + ")  duración " + entrada.getDuracion();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Estilos.NORMAL);
        if (resultado == null || resultado.getGantt().isEmpty()) {
            g2.setColor(Estilos.SUAVE);
            g2.drawString("Ejecute la planificación para ver el diagrama de Gantt.", 16, 36);
            return;
        }
        int y = 28;
        int alto = 42;
        int x0 = 16;
        int unidad = ancho();
        for (EntradaGantt entrada : resultado.getGantt()) {
            int x = x0 + entrada.getInicio() * unidad;
            int w = Math.max(entrada.getDuracion() * unidad, 1);
            g2.setColor(Colores.dePid(entrada.getPid()));
            g2.fillRect(x, y, w, alto);
            g2.setColor(Color.WHITE);
            g2.drawRect(x, y, w, alto);
            if (w > 18) {
                g2.setColor(entrada.getPid() == 0 ? Estilos.TEXTO : Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String texto = entrada.getPid() == 0 ? "OCIOSO" : entrada.getNombre();
                if (fm.stringWidth(texto) < w - 6) {
                    g2.drawString(texto, x + 4, y + 26);
                }
            }
        }
        g2.setColor(Estilos.SUAVE);
        int tiempo = Math.max(resultado.getTiempoTotal(), ultimoFin());
        int paso = tiempo <= 32 ? 1 : tiempo <= 64 ? 2 : 5;
        for (int t = 0; t <= tiempo; t += paso) {
            int x = x0 + t * unidad;
            g2.drawLine(x, y + alto, x, y + alto + 6);
            g2.drawString(Integer.toString(t), x - 2, y + alto + 20);
        }
    }

    private int ultimoFin() {
        int fin = 0;
        for (EntradaGantt entrada : resultado.getGantt()) {
            fin = Math.max(fin, entrada.getFin());
        }
        return fin;
    }

    private int ancho() {
        int tiempo = resultado == null ? 0 : resultado.getTiempoTotal();
        if (tiempo <= 40) return 28;
        if (tiempo <= 80) return 18;
        return 12;
    }

    private EntradaGantt en(int x) {
        if (resultado == null) return null;
        int unidad = ancho();
        int relativo = x - 16;
        if (relativo < 0) return null;
        List<EntradaGantt> gantt = resultado.getGantt();
        for (EntradaGantt entrada : gantt) {
            int inicio = entrada.getInicio() * unidad;
            int fin = entrada.getFin() * unidad;
            if (relativo >= inicio && relativo < fin) return entrada;
        }
        return null;
    }
}
