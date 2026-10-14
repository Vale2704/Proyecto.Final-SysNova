
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

final class PanelMemoria extends JPanel {
    private final Sistema sistema;
    private final JSpinner paginas = new JSpinner(new SpinnerNumberModel(64, 1, 512, 1));
    private final JComboBox<AlgoritmoMemoria> algoritmo = new JComboBox<>(AlgoritmoMemoria.values());
    private final JSpinner impresora = new JSpinner(new SpinnerNumberModel(2, 0, 20, 1));
    private final JSpinner disco = new JSpinner(new SpinnerNumberModel(2, 0, 20, 1));
    private final JSpinner puerto = new JSpinner(new SpinnerNumberModel(2, 0, 20, 1));
    private final JComboBox<Proceso> procesos = new JComboBox<>();
    private final MapaMemoria mapa = new MapaMemoria();
    private final JLabel resumen = new JLabel(" ");
    private boolean ajustando;

    PanelMemoria(Sistema sistema) {
        this.sistema = sistema;
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        algoritmo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AlgoritmoMemoria alg) setText(alg.getEtiqueta());
                return this;
            }
        });
        add(controles(), BorderLayout.NORTH);
        add(new JScrollPane(mapa), BorderLayout.CENTER);
        resumen.setFont(Estilos.NORMAL);
        add(resumen, BorderLayout.SOUTH);
        algoritmo.addActionListener(e -> {
            if (!ajustando && algoritmo.getSelectedItem() != null) {
                sistema.setAlgoritmoMemoria((AlgoritmoMemoria) algoritmo.getSelectedItem());
            }
        });
    }

    private JPanel controles() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 6));
        panel.setOpaque(false);
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila1.setOpaque(false);
        fila1.add(new JLabel("Páginas totales"));
        fila1.add(paginas);
        fila1.add(boton("Aplicar tamaño", this::aplicarTamano));
        fila1.add(new JLabel("Algoritmo"));
        fila1.add(algoritmo);
        fila1.add(boton("Demostrar fragmentación", this::fragmentacion));
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila2.setOpaque(false);
        fila2.add(new JLabel("Impresora"));
        fila2.add(impresora);
        fila2.add(new JLabel("Disco"));
        fila2.add(disco);
        fila2.add(new JLabel("Puerto"));
        fila2.add(puerto);
        fila2.add(boton("Aplicar recursos", this::aplicarRecursos));
        JPanel fila3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila3.setOpaque(false);
        fila3.add(new JLabel("Proceso"));
        procesos.setPrototypeDisplayValue(new Proceso(99, "Nombre largo de ejemplo", 1, 0, 1, 1, 0, new int[]{0, 0, 0}));
        fila3.add(procesos);
        fila3.add(boton("Asignar memoria", this::asignar));
        fila3.add(boton("Liberar memoria", this::liberar));
        panel.add(fila1);
        panel.add(fila2);
        panel.add(fila3);
        return panel;
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Memoria", JOptionPane.WARNING_MESSAGE);
            }
        });
        return boton;
    }

    private void aplicarTamano() {
        try {
            sistema.fijarMemoria(Estilos.entero(paginas));
        } catch (RuntimeException ex) {
            reflejarConfiguracion();
            throw ex;
        }
    }

    private void aplicarRecursos() {
        try {
            sistema.fijarRecursos(Estilos.entero(impresora), Estilos.entero(disco), Estilos.entero(puerto));
        } catch (RuntimeException ex) {
            reflejarConfiguracion();
            throw ex;
        }
    }

    private void fragmentacion() {
        sistema.demostrarFragmentacion();
        reflejarConfiguracion();
    }

    private void asignar() {
        Proceso proceso = (Proceso) procesos.getSelectedItem();
        if (proceso == null) {
            JOptionPane.showMessageDialog(this, "No hay procesos.");
            return;
        }
        sistema.asignarMemoria(proceso);
    }

    private void liberar() {
        Proceso proceso = (Proceso) procesos.getSelectedItem();
        if (proceso == null) return;
        sistema.liberarMemoria(proceso);
    }

    void reflejarConfiguracion() {
        ajustando = true;
        paginas.setValue(sistema.getMemoria().getTotalPaginas());
        impresora.setValue(sistema.getRecursos().getTotal(0));
        disco.setValue(sistema.getRecursos().getTotal(1));
        puerto.setValue(sistema.getRecursos().getTotal(2));
        algoritmo.setSelectedItem(sistema.getMemoria().getAlgoritmo());
        ajustando = false;
    }

    void refrescar() {
        Tablas.procesos(procesos, sistema.getProcesos());
        GestorMemoria memoria = sistema.getMemoria();
        mapa.setMemoria(memoria);
        int total = memoria.getTotalPaginas();
        resumen.setText("Usadas: " + memoria.paginasUsadas() + " / " + total
                + " páginas (" + (total * Sistema.KB_POR_PAGINA) + " KB)    "
                + "Libres: " + memoria.paginasLibres()
                + "    Huecos: " + memoria.numeroHuecos()
                + "    Hueco mayor: " + memoria.huecoMayor()
                + "    Fragmentación externa: " + memoria.fragmentacionExterna()
                + "    " + sistema.getRecursos().resumen()
                + "    Algoritmo: " + memoria.getAlgoritmo().getEtiqueta());
        resumen.setBorder(BorderFactory.createEmptyBorder(8, 2, 2, 2));
    }
}
