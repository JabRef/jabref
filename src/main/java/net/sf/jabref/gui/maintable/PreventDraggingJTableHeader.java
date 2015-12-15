/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.maintable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

/**
 * Related to <code>MainTable</code> class. <br/>
 * Prevents dragging of the first header column ("#") and shows icons in the table header if an icon has to be set.
 *
 * This might not be the best way to solve this problem. Overriding
 * <code>getDraggedColumn</code> produces some ugly gui dragging artifacts if a
 * user attempts to drag something before the first columns.
 *
 * @author Daniel Waeber
 * @author Fabian Bieker
 * @since 12/2008
 */
class PreventDraggingJTableHeader extends JTableHeader implements TableCellRenderer {

    private final MainTableFormat tableFormat;

    private final TableCellRenderer delegate;

    public PreventDraggingJTableHeader(JTable table, MainTableFormat tableFormat) {
        super(table.getColumnModel());
        this.setTable(table);
        this.tableFormat = tableFormat;
        this.delegate = table.getTableHeader().getDefaultRenderer();
        setupTableHeaderIcons();
    }

    private void setupTableHeaderIcons() {

        Enumeration<TableColumn> columns = columnModel.getColumns();
        while(columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            column.setHeaderRenderer(this);
            MainTableColumn mainTableColumn = tableFormat.getTableColumn(column.getModelIndex());
            column.setHeaderValue(mainTableColumn.getHeaderLabel());
        }

    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int index = columnModel.getColumnIndexAtX(event.getX());
        int realIndex = columnModel.getColumn(index).getModelIndex();
        MainTableColumn column = tableFormat.getTableColumn(realIndex);
        return column.getDisplayName();
    }

    /**
     * Overridden to prevent dragging of first column ("#")
     */
    @Override
    public void setDraggedColumn(TableColumn column) {

        if (column != null) {
            // prevent dragging of "#"
            if (column.getModelIndex() == 0) {
                return;
            }
        }
        super.setDraggedColumn(column);
    }

    /**
     * Overridden to prevent dragging of an other column before the first column ("#").
     */
    @Override
    public TableColumn getDraggedColumn() {
        TableColumn column = super.getDraggedColumn();
        if (column != null) {
            PreventDraggingJTableHeader.preventDragBeforeNumberColumn(this.getTable(), column.getModelIndex());
        }

        return column;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // delegate to previously used TableCellRenderer which styles the component
        Component resultFromDelegate = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Changing style is only possible if both value and resultFromDelegate are JLabels
        if (value instanceof JLabel && resultFromDelegate instanceof JLabel) {
            String text = ((JLabel) value).getText();
            Icon icon = ((JLabel) value).getIcon();
            if (icon != null) {
                ((JLabel) resultFromDelegate).setIcon(icon);
                ((JLabel) resultFromDelegate).setText(null);
            } else {
                ((JLabel) resultFromDelegate).setText(text);
            }
        }

        return resultFromDelegate;
    }

    /**
     * Transform model index <code>modelIndex</code> to a view based index and
     * prevent dragging before model index <code>toIndex</code> (inclusive).
     */
    private static void preventDragBeforeNumberColumn(JTable table, int modelIndex) {

        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {

            TableColumn col = table.getColumnModel().getColumn(columnIndex);

            // found the element in the view ...
            // ... and check if it should not be dragged
            if ((col.getModelIndex() == modelIndex) && (columnIndex < 1)) {
                // prevent dragging (move it back ...)
                table.getColumnModel().moveColumn(columnIndex, 1);
                return; // we are done now
            }

        }
    }
}
