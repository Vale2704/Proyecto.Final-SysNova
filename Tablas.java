

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.util.List;

final class Tablas {
    private Tablas() {}

    static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static void instalarEstado(JTable tabla) {
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && value != null) {
                    c.setBackground(Estilos.colorEstado(value.toString()));
                }
                return c;
            }
        });
        tabla.setRowHeight(22);
        tabla.setFont(Estilos.NORMAL);
        tabla.getTableHeader().setFont(Estilos.NEGRITA);
    }

    static void procesos(JComboBox<Proceso> combo, List<Proceso> procesos) {
        if (combo.isPopupVisible()) {
            return;
        }
        Object seleccionado = combo.getSelectedItem();
        int pid = seleccionado instanceof Proceso p ? p.getPid() : -1;
        combo.removeAllItems();
        Proceso volver = null;
        for (Proceso proceso : procesos) {
            combo.addItem(proceso);
            if (proceso.getPid() == pid) {
                volver = proceso;
            }
        }
        if (volver != null) {
            combo.setSelectedItem(volver);
        }
    }

    static void texto(javax.swing.JTextArea area, String texto) {
        if (!texto.equals(area.getText())) {
            area.setText(texto);
            area.setCaretPosition(area.getDocument().getLength());
        }
    }
}
