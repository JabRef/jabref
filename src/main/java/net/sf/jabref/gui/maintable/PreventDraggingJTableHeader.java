/*  Copyright (C) 2003-2016 JabRef contributors.
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

import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

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
class PreventDraggingJTableHeader extends JTableHeader {

    private final MainTableFormat tableFormat;

    public PreventDraggingJTableHeader(JTable table, MainTableFormat tableFormat) {
        super(table.getColumnModel());
        this.setTable(table);
        this.tableFormat = tableFormat;
        setupTableHeaderIcons();
    }

    private void setupTableHeaderIcons() {

        Enumeration<TableColumn> columns = columnModel.getColumns();
        for (TableColumn column : Collections.list(columns)) {
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

        if ((column != null) && (column.getModelIndex() == 0)) {
            return;
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
