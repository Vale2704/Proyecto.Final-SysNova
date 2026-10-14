

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

final class PanelExcepciones extends JPanel {
    private final Sistema sistema;
    private final JComboBox<Proceso> procesos = new JComboBox<>();
    private final JSpinner dividendo = new JSpinner(new SpinnerNumberModel(10, -1000, 1000, 1));
    private final JSpinner divisor = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    private final JCheckBox recuperarDivision = new JCheckBox("Recuperar y continuar (no terminar)", false);
    private final JSpinner pagina = new JSpinner(new SpinnerNumberModel(0, -5, 512, 1));
    private final JCheckBox recuperarAcceso = new JCheckBox("Recuperar y continuar (no terminar)", false);
    private final JTextArea detalle = new JTextArea(4, 40);
    private final DefaultTableModel modelo = Tablas.modelo("Tiempo", "PID", "Proceso", "Tipo", "Manejo", "Detalle");

    PanelExcepciones(Sistema sistema) {
        this.sistema = sistema;
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(controles(), BorderLayout.NORTH);
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.setFont(Estilos.NORMAL);
        detalle.setEditable(false);
        detalle.setFont(Estilos.MONO);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        Estilos.fondo(centro);
        centro.add(new JScrollPane(detalle), BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        JLabel nota = new JLabel("<html>La división ejecuta de verdad <code>dividendo / divisor</code> y captura "
                + "<b>ArithmeticException</b>. El acceso pasa por el gestor de memoria y captura "
                + "<b>AccesoMemoriaInvalidoException</b> si la página no pertenece al proceso o si ya no tiene marcos. "
                + "En modo fatal se liberan memoria y recursos.</html>");
        nota.setForeground(Estilos.SUAVE);
        add(nota, BorderLayout.SOUTH);
    }

    private JPanel controles() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 4));
        panel.setOpaque(false);
        JPanel f1 = fila();
        f1.add(new JLabel("Proceso"));
        f1.add(procesos);
        f1.add(boton("Preparar página inválida", this::prepararInvalida));
        JPanel f2 = fila();
        f2.add(new JLabel("Dividendo"));
        f2.add(dividendo);
        f2.add(new JLabel("Divisor"));
        f2.add(divisor);
        recuperarDivision.setOpaque(false);
        f2.add(recuperarDivision);
        f2.add(boton("Ejecutar división", this::dividir));
        JPanel f3 = fila();
        f3.add(new JLabel("Página lógica"));
        f3.add(pagina);
        recuperarAcceso.setOpaque(false);
        f3.add(recuperarAcceso);
        f3.add(boton("Acceder a memoria", this::acceder));
        f3.add(boton("Acceso válido a la página 0", this::accesoValido));
        panel.add(f1);
        panel.add(f2);
        panel.add(f3);
        return panel;
    }

    private JPanel fila() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Excepciones", JOptionPane.WARNING_MESSAGE);
            }
        });
        return boton;
    }

    private Proceso actual() {
        Proceso proceso = (Proceso) procesos.getSelectedItem();
        if (proceso == null) {
            throw new IllegalArgumentException("Seleccione un proceso.");
        }
        return proceso;
    }

    private void prepararInvalida() {
        Proceso proceso = actual();
        pagina.setValue(proceso.getPaginas());
        detalle.setText("La página " + proceso.getPaginas() + " está fuera del espacio de " + proceso.getEtiqueta()
                + " (válidas: 0 a " + (proceso.getPaginas() - 1) + "). Pulse «Acceder a memoria».");
    }

    private void dividir() {
        String texto = sistema.ejecutarDivision(actual(), Estilos.entero(dividendo), Estilos.entero(divisor),
                recuperarDivision.isSelected());
        detalle.setText(texto);
    }

    private void acceder() {
        String texto = sistema.ejecutarAcceso(actual(), Estilos.entero(pagina), recuperarAcceso.isSelected());
        detalle.setText(texto);
    }

    private void accesoValido() {
        Proceso proceso = actual();
        if (!proceso.tieneMemoria()) {
            sistema.asignarMemoria(proceso);
        }
        pagina.setValue(0);
        recuperarAcceso.setSelected(false);
        String texto = sistema.ejecutarAcceso(proceso, 0, true);
        detalle.setText(texto);
    }

    void refrescar() {
        Tablas.procesos(procesos, sistema.getProcesos());
        modelo.setRowCount(0);
        for (RegistroExcepcion registro : sistema.getExcepciones()) {
            modelo.addRow(new Object[]{
                    registro.getTiempo(), registro.getPid(), registro.getProceso(),
                    registro.getTipo(), registro.getManejo(), registro.getMensaje()
            });
        }
    }
}
