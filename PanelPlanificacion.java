

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.List;

final class PanelPlanificacion extends JPanel {
    private static final DecimalFormat DOS = new DecimalFormat("0.00");
    private static final DecimalFormat TRES = new DecimalFormat("0.000");

    private final Sistema sistema;
    private final JComboBox<AlgoritmoCPU> algoritmo = new JComboBox<>(AlgoritmoCPU.values());
    private final JSpinner quantum = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));
    private final JSpinner aging = new JSpinner(new SpinnerNumberModel(2, 1, 30, 1));
    private final JSpinner velocidad = new JSpinner(new SpinnerNumberModel(140, 40, 1000, 20));
    private final JCheckBox limitar = new JCheckBox("Respetar memoria y recursos al admitir", true);
    private final PanelGantt gantt = new PanelGantt();
    private final DefaultTableModel metricas = Tablas.modelo(
            "PID", "Proceso", "Espera", "Retorno", "Respuesta", "Bloqueado", "Inicio", "Fin");
    private final DefaultTableModel comparacion = Tablas.modelo(
            "Algoritmo", "Espera prom.", "Retorno prom.", "Respuesta prom.", "CPU %", "Throughput", "Tiempo");
    private final JTable tablaMetricas = new JTable(metricas);
    private final JTable tablaComparacion = new JTable(comparacion);
    private final JLabel resumen = new JLabel(" ");
    private final Timer reloj;
    private boolean ajustando;
    private boolean enVivo = true;

    PanelPlanificacion(Sistema sistema) {
        this.sistema = sistema;
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        algoritmo.setRenderer(new EtiquetaAlgoritmo());
        algoritmo.setSelectedItem(AlgoritmoCPU.FCFS);
        limitar.setOpaque(false);
        add(controles(), BorderLayout.NORTH);

        Tablas.instalarEstado(tablaMetricas);
        tablaComparacion.setRowHeight(22);
        tablaComparacion.setFont(Estilos.NORMAL);
        tablaComparacion.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarComparacionSeleccionada();
            }
        });
        JPanel sur = new JPanel(new BorderLayout(0, 6));
        Estilos.fondo(sur);
        JScrollPane scrollComparacion = new JScrollPane(tablaComparacion);
        scrollComparacion.setPreferredSize(new java.awt.Dimension(100, 150));
        sur.add(new JScrollPane(tablaMetricas), BorderLayout.CENTER);
        sur.add(scrollComparacion, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(gantt), sur);
        split.setResizeWeight(0.46);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        resumen.setFont(Estilos.NORMAL);
        resumen.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        JPanel pie = new JPanel(new BorderLayout());
        Estilos.fondo(pie);
        JLabel formula = new JLabel("<html>Espera = tiempo en Listo. &nbsp; Retorno = fin − llegada. &nbsp; "
                + "Respuesta = primer despacho − llegada. &nbsp; Utilización = CPU ocupada / tiempo total. &nbsp; "
                + "Throughput = terminados / tiempo total. &nbsp; En prioridad, el número mayor gana y envejece en la cola de listos.</html>");
        formula.setForeground(Estilos.SUAVE);
        pie.add(resumen, BorderLayout.NORTH);
        pie.add(formula, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);

        reloj = new Timer(140, e -> tick());
        algoritmo.addActionListener(e -> { if (!ajustando) aplicarConfiguracion(); });
        quantum.addChangeListener(e -> { if (!ajustando) aplicarConfiguracion(); });
        aging.addChangeListener(e -> { if (!ajustando) aplicarConfiguracion(); });
        limitar.addActionListener(e -> { if (!ajustando) aplicarConfiguracion(); });
        aplicarConfiguracion();
    }

    private JPanel controles() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        panel.add(new JLabel("Algoritmo"));
        panel.add(algoritmo);
        panel.add(new JLabel("Quantum"));
        panel.add(quantum);
        panel.add(new JLabel("Envejecimiento"));
        panel.add(aging);
        panel.add(limitar);
        panel.add(boton("Ejecutar", this::ejecutar));
        panel.add(boton("Pausa", () -> reloj.stop()));
        panel.add(boton("Paso", this::paso));
        panel.add(boton("Reiniciar", this::reiniciar));
        panel.add(boton("Comparar los 5", this::comparar));
        panel.add(new JLabel("ms"));
        panel.add(velocidad);
        return panel;
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Planificación", JOptionPane.WARNING_MESSAGE);
            }
        });
        return boton;
    }

    private void aplicarConfiguracion() {
        sistema.setAlgoritmo((AlgoritmoCPU) algoritmo.getSelectedItem());
        sistema.setQuantum(Estilos.entero(quantum));
        sistema.setIntervaloAging(Estilos.entero(aging));
        sistema.setLimitarMemoriaRecursos(limitar.isSelected());
    }

    void reflejarConfiguracion() {
        ajustando = true;
        algoritmo.setSelectedItem(sistema.getAlgoritmo());
        quantum.setValue(sistema.getQuantum());
        aging.setValue(sistema.getIntervaloAging());
        limitar.setSelected(sistema.isLimitarMemoriaRecursos());
        ajustando = false;
    }

    private void ejecutar() {
        aplicarConfiguracion();
        enVivo = true;
        sistema.iniciarSimulacion();
        reloj.setDelay(Estilos.entero(velocidad));
        reloj.start();
    }

    private void paso() {
        reloj.stop();
        aplicarConfiguracion();
        enVivo = true;
        PasoSimulacion resultado = sistema.paso();
        if (resultado != PasoSimulacion.AVANZO) {
            reloj.stop();
        }
        mostrar(sistema.getVista());
    }

    private void reiniciar() {
        reloj.stop();
        sistema.detenerSimulacion();
        enVivo = true;
        gantt.setResultado(null);
        metricas.setRowCount(0);
        resumen.setText("Simulación reiniciada.");
    }

    private void comparar() {
        reloj.stop();
        aplicarConfiguracion();
        List<ResultadoPlanificacion> resultados = sistema.comparar();
        comparacion.setRowCount(0);
        for (ResultadoPlanificacion r : resultados) {
            comparacion.addRow(new Object[]{
                    r.getAlgoritmo().getEtiqueta(),
                    DOS.format(r.getEsperaPromedio()),
                    DOS.format(r.getRetornoPromedio()),
                    DOS.format(r.getRespuestaPromedio()),
                    DOS.format(r.getUtilizacionCpu()),
                    TRES.format(r.getThroughput()),
                    r.getTiempoTotal()
            });
        }
        enVivo = false;
        int fila = Math.max(0, algoritmo.getSelectedIndex());
        tablaComparacion.setRowSelectionInterval(fila, fila);
        mostrar(resultados.get(fila));
    }

    private void tick() {
        reloj.setDelay(Estilos.entero(velocidad));
        if (!sistema.tieneSimulacion()) {
            reloj.stop();
            return;
        }
        PasoSimulacion resultado = sistema.paso();
        if (resultado != PasoSimulacion.AVANZO) {
            reloj.stop();
        }
    }

    private void mostrarComparacionSeleccionada() {
        int fila = tablaComparacion.getSelectedRow();
        List<ResultadoPlanificacion> resultados = sistema.getComparacion();
        if (fila < 0 || fila >= resultados.size()) return;
        reloj.stop();
        enVivo = false;
        mostrar(resultados.get(fila));
    }

    void refrescar() {
        if (enVivo) {
            mostrar(sistema.getVista());
        }
    }

    private void mostrar(ResultadoPlanificacion resultado) {
        gantt.setResultado(resultado);
        metricas.setRowCount(0);
        if (resultado == null) {
            resumen.setText(" ");
            return;
        }
        for (Proceso p : resultado.getProcesos()) {
            metricas.addRow(new Object[]{
                    p.getPid(), p.getNombre(), p.getTiempoEspera(),
                    p.getRetorno() < 0 ? "—" : p.getRetorno(),
                    p.getRespuesta() < 0 ? "—" : p.getRespuesta(),
                    p.getTiempoBloqueado(),
                    p.getInicioCpu() < 0 ? "—" : p.getInicioCpu(),
                    p.getFin() < 0 ? "—" : p.getFin()
            });
        }
        String extra = resultado.getMensaje() == null || resultado.getMensaje().isBlank() ? "" : "   " + resultado.getMensaje();
        resumen.setText(resultado.getAlgoritmo().getEtiqueta()
                + "   espera " + DOS.format(resultado.getEsperaPromedio())
                + "   retorno " + DOS.format(resultado.getRetornoPromedio())
                + "   respuesta " + DOS.format(resultado.getRespuestaPromedio())
                + "   CPU " + DOS.format(resultado.getUtilizacionCpu()) + "%"
                + "   throughput " + TRES.format(resultado.getThroughput())
                + "   tiempo " + resultado.getTiempoTotal()
                + "   terminados " + resultado.getCompletados()
                + extra);
    }

    void detener() {
        reloj.stop();
    }

    private static final class EtiquetaAlgoritmo extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                               boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AlgoritmoCPU alg) {
                setText(alg.getEtiqueta());
            }
            return this;
        }
    }
}
