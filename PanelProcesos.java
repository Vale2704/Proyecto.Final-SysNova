

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.List;

final class PanelProcesos extends JPanel {
    private final Sistema sistema;
    private final Runnable alCargarEjemplo;
    private final JTextField nombre = new JTextField("Proceso");
    private final JSpinner prioridad = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final JSpinner llegada = new JSpinner(new SpinnerNumberModel(0, 0, 500, 1));
    private final JSpinner rafaga = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JSpinner paginas = new JSpinner(new SpinnerNumberModel(4, 1, 512, 1));
    private final JSpinner es = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
    private final JSpinner r1 = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
    private final JSpinner r2 = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
    private final JSpinner r3 = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
    private final JComboBox<String> ejemplos = new JComboBox<>(new String[]{
            "Referencia clásica (P1–P4)",
            "Con entrada y salida",
            "Memoria y recursos",
            "Fragmentación externa"
    });
    private final JTable tabla = new JTable();
    private final javax.swing.table.DefaultTableModel modelo = Tablas.modelo(
            "PID", "Nombre", "Prioridad", "Llegada", "Ráfaga", "E/S", "Páginas", "Rec. máx.", "Estado", "Causa", "Memoria");
    private final JTextArea bitacora = new JTextArea();

    PanelProcesos(Sistema sistema, Runnable alCargarEjemplo) {
        this.sistema = sistema;
        this.alCargarEjemplo = alCargarEjemplo;
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(formulario(), BorderLayout.WEST);
        tabla.setModel(modelo);
        Tablas.instalarEstado(tabla);
        tabla.setAutoCreateRowSorter(true);
        bitacora.setEditable(false);
        bitacora.setFont(Estilos.MONO);
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        JScrollPane log = new JScrollPane(bitacora);
        log.setPreferredSize(new Dimension(100, 140));
        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), log);
        centro.setResizeWeight(0.72);
        centro.setBorder(null);
        add(centro, BorderLayout.CENTER);
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mostrarPcb();
                }
            }
        });
    }

    private JScrollPane formulario() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(new JLabel("Nuevo proceso"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(Estilos.fila("Nombre", nombre));
        panel.add(Estilos.fila("Prioridad (1-10)", prioridad));
        panel.add(Estilos.fila("Llegada", llegada));
        panel.add(Estilos.fila("Ráfaga de CPU", rafaga));
        panel.add(Estilos.fila("Páginas", paginas));
        panel.add(Estilos.fila("Ráfaga de E/S", es));
        panel.add(Estilos.fila("Máx. impresora", r1));
        panel.add(Estilos.fila("Máx. disco", r2));
        panel.add(Estilos.fila("Máx. puerto", r3));
        panel.add(Box.createVerticalStrut(8));
        panel.add(boton("Crear proceso", this::crear));
        panel.add(Box.createVerticalStrut(10));
        panel.add(Estilos.fila("Cantidad", cantidad));
        panel.add(boton("Generar aleatorios", this::aleatorios));
        panel.add(Box.createVerticalStrut(10));
        panel.add(ejemplos);
        ejemplos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ejemplos.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(4));
        panel.add(boton("Cargar ejemplo", this::ejemplo));
        panel.add(boton("Cargar CSV…", () -> archivo("csv")));
        panel.add(boton("Cargar JSON…", () -> archivo("json")));
        panel.add(Box.createVerticalStrut(10));
        panel.add(boton("Terminar seleccionado", this::terminar));
        panel.add(boton("Vaciar procesos", this::vaciar));
        JLabel ayuda = new JLabel("<html><body style='width:280px'>Doble clic en un proceso para ver su PCB. "
                + "La prioridad usa número mayor = más prioritario. Los recursos son el máximo que el proceso reservará al ser admitido.</body></html>");
        ayuda.setForeground(Estilos.SUAVE);
        ayuda.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(8));
        panel.add(ayuda);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(340, 480));
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setAlignmentX(LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        boton.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo completar", JOptionPane.WARNING_MESSAGE);
            }
        });
        return boton;
    }

    private void crear() {
        sistema.crear(nombre.getText(), Estilos.entero(prioridad), Estilos.entero(llegada),
                Estilos.entero(rafaga), Estilos.entero(paginas), Estilos.entero(es),
                new int[]{Estilos.entero(r1), Estilos.entero(r2), Estilos.entero(r3)});
    }

    private void aleatorios() {
        sistema.crearAleatorios(Estilos.entero(cantidad));
    }

    private void ejemplo() {
        if (!sistema.getProcesos().isEmpty() && JOptionPane.showConfirmDialog(this,
                "La carga de ejemplo reemplaza los procesos actuales.", "Cargar ejemplo",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        switch (ejemplos.getSelectedIndex()) {
            case 0 -> sistema.cargarReferencia();
            case 1 -> sistema.cargarConEntradaSalida();
            case 2 -> sistema.cargarMemoriaYRecursos();
            default -> sistema.demostrarFragmentacion();
        }
        if (alCargarEjemplo != null) {
            alCargarEjemplo.run();
        }
    }

    private void archivo(String extension) {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(), extension));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        if (!sistema.getProcesos().isEmpty() && JOptionPane.showConfirmDialog(this,
                "La carga reemplaza los procesos actuales.", "Cargar archivo",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            var datos = CargadorProcesos.leer(chooser.getSelectedFile().toPath());
            int n = sistema.cargar(datos, true);
            sistema.anotar("Se cargaron " + n + " procesos desde " + chooser.getSelectedFile().getName() + ".");
        } catch (Exception ex) {
            String mensaje = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            JOptionPane.showMessageDialog(this, mensaje, "Archivo no válido", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void terminar() {
        Proceso proceso = seleccionado();
        if (proceso == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un proceso de la tabla.");
            return;
        }
        sistema.terminar(proceso);
    }

    private void vaciar() {
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar todos los procesos y liberar la memoria?",
                "Vaciar", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            sistema.vaciar();
        }
    }

    private Proceso seleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) return null;
        int fila = tabla.convertRowIndexToModel(vista);
        int pid = (Integer) modelo.getValueAt(fila, 0);
        for (Proceso proceso : sistema.getProcesos()) {
            if (proceso.getPid() == pid) return proceso;
        }
        return null;
    }

    private void mostrarPcb() {
        Proceso p = seleccionado();
        if (p == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("PID: ").append(p.getPid()).append('\n');
        sb.append("Nombre: ").append(p.getNombre()).append('\n');
        sb.append("Prioridad base / actual: ").append(p.getPrioridadBase()).append(" / ").append(p.getPrioridad()).append('\n');
        sb.append("Llegada: ").append(p.getLlegada()).append("    Ráfaga CPU: ").append(p.getRafaga())
                .append("    Restante: ").append(p.getRestante()).append('\n');
        sb.append("Páginas: ").append(p.getPaginas()).append("    Base: ")
                .append(p.tieneMemoria() ? p.getBaseMemoria() : "sin asignar").append('\n');
        sb.append("Ráfaga E/S: ").append(p.getRafagaES()).append('\n');
        sb.append("Recursos máximos: ").append(p.recursosMaxTexto()).append('\n');
        sb.append("Recursos asignados: ").append(p.recursosAsignadosTexto()).append('\n');
        sb.append("Estado: ").append(p.getEstado().getEtiqueta());
        if (!p.getMotivo().getEtiqueta().isEmpty()) {
            sb.append(" (").append(p.getMotivo().getEtiqueta()).append(')');
        }
        sb.append("\nÚltima causa: ").append(p.getUltimaCausa()).append('\n');
        sb.append("Espera: ").append(p.getTiempoEspera())
                .append("    Retorno: ").append(p.getRetorno() < 0 ? "—" : p.getRetorno())
                .append("    Respuesta: ").append(p.getRespuesta() < 0 ? "—" : p.getRespuesta())
                .append("    Bloqueado: ").append(p.getTiempoBloqueado()).append('\n');
        if (!p.getUltimoMensaje().isEmpty()) {
            sb.append("Último mensaje: ").append(p.getUltimoMensaje()).append('\n');
        }
        sb.append("\nTransiciones\n");
        for (Transicion t : sistema.getTransiciones()) {
            if (t.getPid() == p.getPid()) {
                sb.append(String.format("%4d   %-12s → %-12s   %s%n",
                        t.getTiempo(), t.getDesdeTexto(), t.getHaciaTexto(), t.getCausa()));
            }
        }
        JTextArea area = new JTextArea(sb.toString(), 20, 64);
        area.setEditable(false);
        area.setFont(Estilos.MONO);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "PCB " + p.getEtiqueta(), JOptionPane.INFORMATION_MESSAGE);
    }

    void refrescar() {
        List<Proceso> procesos = sistema.getProcesos();
        modelo.setRowCount(0);
        for (Proceso p : procesos) {
            modelo.addRow(new Object[]{
                    p.getPid(), p.getNombre(), p.getPrioridad(), p.getLlegada(), p.getRafaga(),
                    p.getRafagaES(), p.getPaginas(), p.recursosMaxTexto(), p.getEstado().getEtiqueta(),
                    p.getUltimaCausa(),
                    p.tieneMemoria() ? "base " + p.getBaseMemoria() : "—"
            });
        }
        StringBuilder sb = new StringBuilder();
        for (String linea : sistema.getBitacora()) {
            sb.append(linea).append('\n');
        }
        Tablas.texto(bitacora, sb.toString());
    }
}
