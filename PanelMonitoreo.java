

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

final class PanelMonitoreo extends JPanel {
    private final Sistema sistema;
    private final JLabel reloj = tarjeta();
    private final JLabel cpu = tarjeta();
    private final JLabel memoriaTxt = tarjeta();
    private final JProgressBar barra = new JProgressBar();
    private final JLabel[] estados = new JLabel[EstadoProceso.values().length];
    private final DefaultTableModel procesos = Tablas.modelo(
            "PID", "Nombre", "Estado", "Motivo", "Prioridad", "Restante", "Espera", "Páginas", "Base");
    private final DefaultTableModel transiciones = Tablas.modelo(
            "Tiempo", "PID", "Proceso", "Desde", "Hacia", "Causa");

    PanelMonitoreo(Sistema sistema) {
        this.sistema = sistema;
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(cabecera(), BorderLayout.NORTH);
        JTable tablaProcesos = new JTable(procesos);
        JTable tablaTransiciones = new JTable(transiciones);
        Tablas.instalarEstado(tablaProcesos);
        tablaTransiciones.setRowHeight(22);
        tablaTransiciones.setFont(Estilos.NORMAL);
        tablaTransiciones.getTableHeader().setFont(Estilos.NEGRITA);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(tablaProcesos), new JScrollPane(tablaTransiciones));
        split.setResizeWeight(0.48);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private JPanel cabecera() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        JPanel cifras = new JPanel(new GridLayout(1, 3, 8, 0));
        cifras.setOpaque(false);
        cifras.add(caja("Reloj", reloj));
        cifras.add(caja("CPU", cpu));
        JPanel mem = caja("Memoria", memoriaTxt);
        mem.add(barra, BorderLayout.SOUTH);
        barra.setStringPainted(true);
        cifras.add(mem);
        JPanel filaEstados = new JPanel(new GridLayout(1, estados.length, 8, 0));
        filaEstados.setOpaque(false);
        EstadoProceso[] valores = EstadoProceso.values();
        for (int i = 0; i < valores.length; i++) {
            estados[i] = new JLabel("0", javax.swing.SwingConstants.CENTER);
            estados[i].setFont(new Font("Segoe UI", Font.BOLD, 22));
            estados[i].setOpaque(true);
            estados[i].setBackground(Estilos.colorEstado(valores[i].getEtiqueta()));
            estados[i].setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            JPanel celda = new JPanel(new BorderLayout());
            celda.setOpaque(false);
            celda.add(estados[i], BorderLayout.CENTER);
            JLabel nombre = new JLabel(valores[i].getEtiqueta(), javax.swing.SwingConstants.CENTER);
            nombre.setFont(Estilos.NORMAL);
            celda.add(nombre, BorderLayout.SOUTH);
            filaEstados.add(celda);
        }
        panel.add(cifras, BorderLayout.NORTH);
        panel.add(filaEstados, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel tarjeta() {
        JLabel label = new JLabel("—");
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(Estilos.TEXTO);
        return label;
    }

    private JPanel caja(String titulo, JLabel valor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.LINEA),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JLabel t = new JLabel(titulo);
        t.setForeground(Estilos.SUAVE);
        panel.add(t, BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);
        return panel;
    }

    void refrescar() {
        reloj.setText(Integer.toString(sistema.getReloj()));
        Proceso enCpu = sistema.procesoEnEjecucion();
        cpu.setText(enCpu == null ? "Ociosa" : enCpu.getEtiqueta() + " " + enCpu.getNombre());
        int total = sistema.getMemoria().getTotalPaginas();
        int usadas = sistema.getMemoria().paginasUsadas();
        memoriaTxt.setText(usadas + " / " + total + " pág.");
        barra.setMaximum(Math.max(1, total));
        barra.setValue(usadas);
        barra.setString(total == 0 ? "0%" : (usadas * 100 / total) + "%");
        EstadoProceso[] valores = EstadoProceso.values();
        for (int i = 0; i < valores.length; i++) {
            estados[i].setText(Integer.toString(sistema.contar(valores[i])));
        }
        procesos.setRowCount(0);
        for (Proceso p : sistema.getProcesos()) {
            procesos.addRow(new Object[]{
                    p.getPid(), p.getNombre(), p.getEstado().getEtiqueta(),
                    p.getMotivo().getEtiqueta().isEmpty() ? "—" : p.getMotivo().getEtiqueta(),
                    p.getPrioridad(), p.getRestante(), p.getTiempoEspera(), p.getPaginas(),
                    p.tieneMemoria() ? p.getBaseMemoria() : "—"
            });
        }
        transiciones.setRowCount(0);
        for (Transicion t : sistema.getTransiciones()) {
            transiciones.addRow(new Object[]{
                    t.getTiempo(), t.getPid(), t.getProceso(), t.getDesdeTexto(), t.getHaciaTexto(), t.getCausa()
            });
        }
    }
}
