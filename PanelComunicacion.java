

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

final class PanelComunicacion extends JPanel {
    private final Sistema sistema;
    private final JComboBox<Proceso> emisor = new JComboBox<>();
    private final JComboBox<Proceso> receptor = new JComboBox<>();
    private final JComboBox<String> semaforos = new JComboBox<>();
    private final JTextField nombreSem = new JTextField("mutex", 10);
    private final JSpinner valorSem = new JSpinner(new SpinnerNumberModel(1, 0, 20, 1));
    private final JTextField mensaje = new JTextField("dato listo", 16);
    private final DefaultTableModel modelo = Tablas.modelo("Semáforo", "Valor", "Inicial", "En espera");
    private final JTextArea bitacora = new JTextArea();
    private boolean receptorListo;

    PanelComunicacion(Sistema sistema) {
        this.sistema = sistema;
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(controles(), BorderLayout.NORTH);
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.setFont(Estilos.NORMAL);
        bitacora.setEditable(false);
        bitacora.setFont(Estilos.MONO);
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), new JScrollPane(bitacora));
        split.setResizeWeight(0.35);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        JLabel nota = new JLabel("<html>Wait con valor 0 deja al proceso en Bloqueado (espera de semáforo). "
                + "Signal despierta al primero de la cola. Receive sobre un buzón vacío bloquea (espera de mensaje) "
                + "y Send lo despierta. Hágalo con la simulación en pausa: el planificador no les dará CPU mientras sigan bloqueados. "
                + "Ejecutar o Reiniciar devuelve los procesos a Nuevo y los semáforos a su valor inicial.</html>");
        nota.setForeground(Estilos.SUAVE);
        add(nota, BorderLayout.SOUTH);
    }

    private JPanel controles() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 4));
        panel.setOpaque(false);
        JPanel f1 = fila();
        f1.add(new JLabel("Proceso A"));
        f1.add(emisor);
        f1.add(new JLabel("Proceso B"));
        f1.add(receptor);
        f1.add(boton("Demostración IPC", this::demostracion));
        JPanel f2 = fila();
        f2.add(new JLabel("Nuevo semáforo"));
        f2.add(nombreSem);
        f2.add(new JLabel("Valor"));
        f2.add(valorSem);
        f2.add(boton("Crear semáforo", this::crearSemaforo));
        JPanel f3 = fila();
        f3.add(new JLabel("Semáforo"));
        f3.add(semaforos);
        f3.add(boton("Wait (proceso A)", this::esperar));
        f3.add(boton("Signal", this::senalar));
        JPanel f4 = fila();
        f4.add(new JLabel("Mensaje"));
        f4.add(mensaje);
        f4.add(boton("Enviar A → B", this::enviar));
        f4.add(boton("Recibir (proceso B)", this::recibir));
        panel.add(f1);
        panel.add(f2);
        panel.add(f3);
        panel.add(f4);
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
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Comunicación", JOptionPane.WARNING_MESSAGE);
            }
        });
        return boton;
    }

    private void crearSemaforo() {
        sistema.crearSemaforo(nombreSem.getText(), Estilos.entero(valorSem));
    }

    private void esperar() {
        sistema.esperarSemaforo(exigir(emisor, "A"), semaforoSeleccionado());
    }

    private void senalar() {
        sistema.senalarSemaforo(semaforoSeleccionado());
    }

    private void enviar() {
        sistema.enviar(exigir(emisor, "A"), exigir(receptor, "B"), mensaje.getText());
    }

    private void recibir() {
        sistema.recibir(exigir(receptor, "B"));
    }

    private void demostracion() {
        sistema.demostracionIpc(exigir(emisor, "A"), exigir(receptor, "B"));
    }

    private Proceso exigir(JComboBox<Proceso> combo, String cual) {
        Proceso proceso = (Proceso) combo.getSelectedItem();
        if (proceso == null) {
            throw new IllegalArgumentException("Seleccione el proceso " + cual + ". Cree al menos dos procesos.");
        }
        return proceso;
    }

    private String semaforoSeleccionado() {
        Object valor = semaforos.getSelectedItem();
        if (valor == null) {
            throw new IllegalArgumentException("Cree un semáforo primero.");
        }
        return valor.toString();
    }

    void refrescar() {
        Tablas.procesos(emisor, sistema.getProcesos());
        Tablas.procesos(receptor, sistema.getProcesos());
        if (!receptorListo && receptor.getItemCount() > 1) {
            receptor.setSelectedIndex(1);
            receptorListo = true;
        }
        if (!semaforos.isPopupVisible()) {
            Object sel = semaforos.getSelectedItem();
            semaforos.removeAllItems();
            for (SemaforoSO semaforo : sistema.getIpc().getSemaforos()) {
                semaforos.addItem(semaforo.getNombre());
            }
            if (sel != null) semaforos.setSelectedItem(sel);
        }
        modelo.setRowCount(0);
        for (SemaforoSO semaforo : sistema.getIpc().getSemaforos()) {
            modelo.addRow(new Object[]{semaforo.getNombre(), semaforo.getValor(), semaforo.getValorInicial(), semaforo.procesosEnEspera()});
        }
        StringBuilder sb = new StringBuilder();
        for (String linea : sistema.getBitacora()) sb.append(linea).append('\n');
        Tablas.texto(bitacora, sb.toString());
    }
}
