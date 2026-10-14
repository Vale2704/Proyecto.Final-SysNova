

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Condición de carrera con hilos reales y su corrección con ReentrantLock y Semaphore. */
final class PanelSincronizacion extends JPanel {
    private final JSpinner hilos = new JSpinner(new SpinnerNumberModel(4, 2, 8, 1));
    private final JSpinner repeticiones = new JSpinner(new SpinnerNumberModel(40, 10, 200, 10));
    private final JLabel resultadoContador = new JLabel(" ");
    private final JSpinner capacidad = new JSpinner(new SpinnerNumberModel(5, 2, 12, 1));
    private final JSpinner productores = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
    private final JSpinner consumidores = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
    private final JSpinner items = new JSpinner(new SpinnerNumberModel(12, 4, 40, 1));
    private final JPanel celdas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel resultadoBuffer = new JLabel(" ");
    private final JTextArea eventos = new JTextArea();
    private final AtomicBoolean ocupado = new AtomicBoolean(false);
    private final List<JButton> botones = new java.util.ArrayList<>();

    PanelSincronizacion() {
        setLayout(new BorderLayout(8, 8));
        Estilos.fondo(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel intro = new JLabel("<html><body style='width:980px'>Productor–Consumidor con hilos de verdad. "
                + "Sin sincronización, dos hilos leen el mismo contador y uno pisa al otro. "
                + "Con sincronización, <b>ReentrantLock</b> es el mutex de la sección crítica y dos "
                + "<b>Semaphore</b> cuentan las casillas vacías y las llenas.</body></html>");
        add(intro, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 12, 0));
        centro.setOpaque(false);
        centro.add(panelContador());
        centro.add(panelBuffer());
        add(centro, BorderLayout.CENTER);
        eventos.setEditable(false);
        eventos.setFont(Estilos.MONO);
        JScrollPane log = new JScrollPane(eventos);
        log.setPreferredSize(new Dimension(100, 180));
        add(log, BorderLayout.SOUTH);
        construirCeldas();
    }

    private JPanel panelContador() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        Estilos.fondo(panel);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.LINEA),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        panel.add(new JLabel("Contador compartido"), BorderLayout.NORTH);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controles.setOpaque(false);
        controles.add(new JLabel("Hilos"));
        controles.add(hilos);
        controles.add(new JLabel("Incrementos"));
        controles.add(repeticiones);
        controles.add(boton("Sin sincronización", () -> correrContador(false)));
        controles.add(boton("Con ReentrantLock", () -> correrContador(true)));
        JPanel cuerpo = new JPanel(new BorderLayout());
        Estilos.fondo(cuerpo);
        cuerpo.add(controles, BorderLayout.NORTH);
        resultadoContador.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cuerpo.add(resultadoContador, BorderLayout.CENTER);
        panel.add(cuerpo, BorderLayout.CENTER);
        return panel;
    }

    private JPanel panelBuffer() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        Estilos.fondo(panel);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.LINEA),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        panel.add(new JLabel("Buffer acotado"), BorderLayout.NORTH);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controles.setOpaque(false);
        controles.add(new JLabel("Casillas"));
        controles.add(capacidad);
        controles.add(new JLabel("Prod."));
        controles.add(productores);
        controles.add(new JLabel("Cons."));
        controles.add(consumidores);
        controles.add(new JLabel("Ítems c/u"));
        controles.add(items);
        controles.add(boton("Buffer sin sincronización", () -> correrBuffer(false)));
        controles.add(boton("Buffer con semáforos y mutex", () -> correrBuffer(true)));
        JPanel cuerpo = new JPanel(new BorderLayout(0, 8));
        Estilos.fondo(cuerpo);
        cuerpo.add(controles, BorderLayout.NORTH);
        celdas.setOpaque(false);
        cuerpo.add(celdas, BorderLayout.CENTER);
        resultadoBuffer.setFont(Estilos.NEGRITA);
        cuerpo.add(resultadoBuffer, BorderLayout.SOUTH);
        panel.add(cuerpo, BorderLayout.CENTER);
        return panel;
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        botones.add(boton);
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    private void construirCeldas() {
        celdas.removeAll();
        int n = Estilos.entero(capacidad);
        for (int i = 0; i < n; i++) {
            JLabel celda = new JLabel("·", javax.swing.SwingConstants.CENTER);
            celda.setOpaque(true);
            celda.setBackground(Colores.LIBRE);
            celda.setPreferredSize(new Dimension(42, 36));
            celda.setBorder(BorderFactory.createLineBorder(Estilos.LINEA));
            celdas.add(celda);
        }
        celdas.revalidate();
        celdas.repaint();
    }

    private void correrContador(boolean sincronizado) {
        if (!ocupado.compareAndSet(false, true)) return;
        habilitar(false);
        int nHilos = Estilos.entero(hilos);
        int nRep = Estilos.entero(repeticiones);
        resultadoContador.setText("Ejecutando " + nHilos + " hilos…");
        Thread hilo = new Thread(() -> {
            try {
                DemoProductorConsumidor.Informe informe = DemoProductorConsumidor.contador(sincronizado, nHilos, nRep);
                SwingUtilities.invokeLater(() -> mostrarContador(informe));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> fallar(ex));
            }
        }, "contador-demo");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void correrBuffer(boolean sincronizado) {
        if (!ocupado.compareAndSet(false, true)) return;
        habilitar(false);
        int cap = Estilos.entero(capacidad);
        int nProd = Estilos.entero(productores);
        int nCons = Estilos.entero(consumidores);
        int nItems = Estilos.entero(items);
        construirCeldas();
        eventos.setText("");
        resultadoBuffer.setText(sincronizado ? "Ejecutando con Semaphore y ReentrantLock…" : "Ejecutando sin sincronización…");
        Thread hilo = new Thread(() -> {
            try {
                DemoProductorConsumidor.Informe informe = DemoProductorConsumidor.buffer(
                        sincronizado, cap, nProd, nCons, nItems,
                        copia -> SwingUtilities.invokeLater(() -> pintar(copia)));
                SwingUtilities.invokeLater(() -> mostrarBuffer(informe));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> fallar(ex));
            }
        }, "buffer-demo");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarContador(DemoProductorConsumidor.Informe informe) {
        int perdidas = Math.max(0, informe.esperado - informe.obtenido);
        resultadoContador.setForeground(perdidas == 0 ? new Color(21, 128, 61) : new Color(153, 27, 27));
        resultadoContador.setText("<html>Esperado: " + informe.esperado + "&nbsp;&nbsp; Obtenido: " + informe.obtenido
                + "&nbsp;&nbsp; Perdidos: " + perdidas + "<br>" + informe.conclusion + "</html>");
        habilitar(true);
    }

    private void mostrarBuffer(DemoProductorConsumidor.Informe informe) {
        pintar(informe.bufferFinal);
        boolean ok = informe.anomalias == 0;
        resultadoBuffer.setForeground(ok ? new Color(21, 128, 61) : new Color(153, 27, 27));
        resultadoBuffer.setText("<html>Producidos: " + informe.producidos + "&nbsp;&nbsp; Consumidos: " + informe.consumidos
                + "&nbsp;&nbsp; Anomalías: " + informe.anomalias + "<br>" + informe.conclusion + "</html>");
        StringBuilder sb = new StringBuilder();
        for (String evento : informe.eventos) sb.append(evento).append('\n');
        eventos.setText(sb.toString());
        habilitar(true);
    }

    private void pintar(int[] datos) {
        if (datos == null) return;
        if (celdas.getComponentCount() != datos.length) {
            capacidad.setValue(datos.length);
            construirCeldas();
        }
        for (int i = 0; i < datos.length && i < celdas.getComponentCount(); i++) {
            JLabel celda = (JLabel) celdas.getComponent(i);
            if (datos[i] == 0) {
                celda.setText("·");
                celda.setBackground(Colores.LIBRE);
            } else {
                celda.setText(Integer.toString(datos[i]));
                celda.setBackground(new Color(187, 247, 208));
            }
        }
    }

    private void fallar(Exception ex) {
        habilitar(true);
        JOptionPane.showMessageDialog(this, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "Sincronización", JOptionPane.WARNING_MESSAGE);
    }

    private void habilitar(boolean habilitado) {
        if (habilitado) ocupado.set(false);
        for (JButton boton : botones) boton.setEnabled(habilitado);
    }
}
