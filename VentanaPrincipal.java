

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public final class VentanaPrincipal extends JFrame {
    private final Sistema sistema = new Sistema();
    private final PanelPlanificacion planificacion;
    private final PanelMemoria memoria;
    private final PanelProcesos procesos;
    private final PanelComunicacion comunicacion;
    private final PanelSincronizacion sincronizacion = new PanelSincronizacion();
    private final PanelMonitoreo monitoreo;
    private final PanelExcepciones excepciones;
    private final JLabel estado = new JLabel(" ");

    public VentanaPrincipal() {
        super("Sistema de Gestión de Procesos y Recursos");
        planificacion = new PanelPlanificacion(sistema);
        memoria = new PanelMemoria(sistema);
        procesos = new PanelProcesos(sistema, this::reflejarConfiguracion);
        comunicacion = new PanelComunicacion(sistema);
        monitoreo = new PanelMonitoreo(sistema);
        excepciones = new PanelExcepciones(sistema);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new Encabezado(), BorderLayout.NORTH);
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(Estilos.NEGRITA);
        pestanas.setBackground(Estilos.FONDO);
        pestanas.addTab("Procesos", procesos);
        pestanas.addTab("Planificación", planificacion);
        pestanas.addTab("Memoria", memoria);
        pestanas.addTab("Comunicación", comunicacion);
        pestanas.addTab("Sincronización", sincronizacion);
        pestanas.addTab("Monitoreo", monitoreo);
        pestanas.addTab("Excepciones", excepciones);
        add(pestanas, BorderLayout.CENTER);
        estado.setFont(Estilos.NORMAL);
        estado.setForeground(new Color(236, 228, 214));
        estado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, Estilos.ACENTO),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        estado.setOpaque(true);
        estado.setBackground(Estilos.HEADER);
        add(estado, BorderLayout.SOUTH);
        getContentPane().setBackground(Estilos.FONDO);
        setJMenuBar(menu());

        sistema.observar(() -> {
            if (SwingUtilities.isEventDispatchThread()) refrescar();
            else SwingUtilities.invokeLater(this::refrescar);
        });
        sistema.cargarReferencia();
        reflejarConfiguracion();
        refrescar();
        setMinimumSize(new Dimension(1020, 680));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                planificacion.detener();
            }
        });
    }

    /** Franja superior: tinta, línea de acento y título del sistema. */
    private static final class Encabezado extends JPanel {
        Encabezado() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(800, 78));
            setBorder(BorderFactory.createEmptyBorder(16, 28, 14, 18));
            JLabel titulo = new JLabel("Sistema de Gestión de Procesos y Recursos");
            titulo.setFont(Estilos.TITULO);
            titulo.setForeground(Color.WHITE);
            JLabel sub = new JLabel("Multiprogramación  ·  CPU, memoria, IPC, sincronización y monitoreo");
            sub.setFont(Estilos.NORMAL);
            sub.setForeground(new Color(214, 206, 186));
            JPanel textos = new JPanel(new java.awt.GridLayout(2, 1, 0, 3));
            textos.setOpaque(false);
            textos.add(titulo);
            textos.add(sub);
            add(textos, BorderLayout.WEST);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(16, 32, 48), getWidth(), getHeight(), new Color(36, 64, 86)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Estilos.ACENTO);
            g2.fillRect(0, getHeight() - 4, getWidth(), 4);
            g2.fillRoundRect(14, 22, 6, 34, 4, 4);
        }
    }

    private JMenuBar menu() {
        JMenuBar barra = new JMenuBar();
        JMenu ayuda = new JMenu("Ayuda");
        JMenuItem cobertura = new JMenuItem("Qué cubre el sistema");
        cobertura.addActionListener(e -> mostrarAyuda());
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> {
            planificacion.detener();
            dispose();
        });
        ayuda.add(cobertura);
        ayuda.add(salir);
        barra.add(ayuda);
        return barra;
    }

    private void mostrarAyuda() {
        JTextArea area = new JTextArea("""
                1. Procesos. Creación manual, aleatoria o desde CSV/JSON.
                   El PCB guarda PID, prioridad, llegada, ráfaga, páginas, recursos máximos y estado.
                2. Planificación. FCFS, SJF, SRTF, Round Robin (quantum configurable) y prioridad con envejecimiento.
                   La misma carga se compara. Hay espera, retorno, respuesta, utilización y throughput, y diagrama de Gantt.
                3. Memoria. First Fit, Best Fit y Worst Fit, en páginas. Al terminar se liberan los marcos y se fusionan huecos.
                4. Comunicación. Semáforos (wait/signal) y paso de mensajes por buzón. Si no hay recurso, el proceso pasa a Bloqueado.
                5. Estados. Nuevo, Listo, Ejecución, Bloqueado y Terminado.
                   Cada transición guarda la causa: admisión, despacho, fin de quantum, expropiación, E/S, espera de recurso, finalización.
                6. Sincronización. Productor–Consumidor con hilos reales.
                   Se ve la carrera sin protección y la corrección con Semaphore y ReentrantLock.
                7. Monitoreo. Estados, CPU y memoria se actualizan mientras la simulación avanza.
                8. Terminación. El botón Terminar libera memoria, recursos y lo saca de las colas de IPC.
                9. Excepciones. División por cero (ArithmeticException) y acceso a una página que no es del proceso.
                   Se puede terminar el proceso o omitir la instrucción.
                10. Interfaz. Estas pestañas son el medio para crear, ejecutar y observar.

                Carga clásica ya incluida: P1 (0,8), P2 (1,4), P3 (2,9), P4 (3,5).
                Con quantum 4, FCFS espera promedio 8.75; SJF 7.75; SRTF 6.50; Round Robin 11.75.
                """, 22, 78);
        area.setEditable(false);
        area.setFont(Estilos.NORMAL);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Cobertura del proyecto", JOptionPane.INFORMATION_MESSAGE);
    }

    private void reflejarConfiguracion() {
        planificacion.reflejarConfiguracion();
        memoria.reflejarConfiguracion();
    }

    private void refrescar() {
        procesos.refrescar();
        planificacion.refrescar();
        memoria.refrescar();
        comunicacion.refrescar();
        monitoreo.refrescar();
        excepciones.refrescar();
        Proceso cpu = sistema.procesoEnEjecucion();
        int total = sistema.getMemoria().getTotalPaginas();
        int usadas = sistema.getMemoria().paginasUsadas();
        int pct = total == 0 ? 0 : usadas * 100 / total;
        String textoCpu = cpu == null ? "ociosa" : cpu.getEtiqueta() + " " + cpu.getNombre();
        String nombreAlgoritmo = sistema.tieneSimulacion() && sistema.getVista() != null
                ? sistema.getVista().getAlgoritmo().getEtiqueta()
                : sistema.getAlgoritmo().getEtiqueta();
        estado.setText("Reloj: " + sistema.getReloj()
                + "    Algoritmo: " + nombreAlgoritmo
                + "    CPU: " + textoCpu
                + "    Memoria: " + usadas + "/" + total + " páginas (" + pct + "%)"
                + "    Procesos: " + sistema.getProcesos().size());
    }
}
