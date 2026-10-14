

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.ParseException;

public final class Estilos {
    public static final Color HEADER = new Color(22, 40, 58);
    public static final Color ACENTO = new Color(184, 112, 40);
    public static final Color FONDO = new Color(246, 243, 236);
    public static final Color LINEA = new Color(214, 206, 190);
    public static final Color TEXTO = new Color(32, 40, 48);
    public static final Color SUAVE = new Color(90, 84, 74);
    public static final Font TITULO = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font NEGRITA = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 12);

    private Estilos() {}

    /** Apariencia del sistema. Debe llamarse antes de crear la ventana. */
    public static void aplicar() {
        UIManager.put("nimbusBase", HEADER);
        UIManager.put("nimbusBlueGrey", new Color(214, 206, 190));
        UIManager.put("control", FONDO);
        UIManager.put("nimbusFocus", ACENTO);
        UIManager.put("nimbusSelectionBackground", new Color(36, 88, 122));
        UIManager.put("text", TEXTO);
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // Se conserva el estilo por defecto de Swing.
            }
        }
        UIManager.put("Button.font", NORMAL);
        UIManager.put("Label.font", NORMAL);
        UIManager.put("ComboBox.font", NORMAL);
        UIManager.put("Table.font", NORMAL);
        UIManager.put("TextField.font", NORMAL);
        UIManager.put("TabbedPane.font", NEGRITA);
        UIManager.put("Menu.font", NORMAL);
        UIManager.put("MenuItem.font", NORMAL);
    }

    public static void fondo(JComponent componente) {
        componente.setOpaque(true);
        componente.setBackground(FONDO);
    }

    public static JPanel fila(String etiqueta, JComponent campo) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);
        JLabel label = new JLabel(etiqueta);
        label.setFont(NORMAL);
        panel.add(label);
        panel.add(campo);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return panel;
    }

    public static int entero(JSpinner spinner) {
        try {
            spinner.commitEdit();
        } catch (ParseException ignored) {
            // Se conserva el último valor válido del modelo.
        }
        return ((Number) spinner.getValue()).intValue();
    }

    public static Color colorEstado(String etiqueta) {
        if (EstadoProceso.NUEVO.getEtiqueta().equals(etiqueta)) return new Color(226, 232, 240);
        if (EstadoProceso.LISTO.getEtiqueta().equals(etiqueta)) return new Color(219, 234, 254);
        if (EstadoProceso.EJECUCION.getEtiqueta().equals(etiqueta)) return new Color(209, 250, 229);
        if (EstadoProceso.BLOQUEADO.getEtiqueta().equals(etiqueta)) return new Color(254, 243, 199);
        if (EstadoProceso.TERMINADO.getEtiqueta().equals(etiqueta)) return new Color(229, 231, 235);
        return Color.WHITE;
    }

    public static JPanel tarjeta(String titulo, JComponent cuerpo) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 6));
        fondo(panel);
        JLabel label = new JLabel(titulo);
        label.setFont(NEGRITA);
        label.setForeground(HEADER);
        panel.add(label, java.awt.BorderLayout.NORTH);
        panel.add(cuerpo, java.awt.BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINEA),
                new EmptyBorder(10, 10, 10, 10)));
        return panel;
    }
}
