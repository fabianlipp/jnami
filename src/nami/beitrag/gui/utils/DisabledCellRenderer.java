package nami.beitrag.gui.utils;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Modifiziert den DefaultRenderer so, dass Steuerelemente disabled werden
 * (enabled=false), wenn die entsprechende Zelle nicht editable ist.
 * 
 * @author Fabian Lipp
 */
public class DisabledCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -4317607684491411756L;

    @Override
    public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {

        TableCellRenderer renderer = table.getDefaultRenderer(table
                .getColumnClass(column));
        Component c = renderer.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);

        // Deaktiviert die Komponente, falls das Feld nicht bearbeitbar ist
        if (!table.isCellEditable(row, column)) {
            c.setEnabled(false);
        }

        return c;
    }
}
