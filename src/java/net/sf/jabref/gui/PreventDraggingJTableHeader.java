package net.sf.jabref.gui;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;

/**
 * Related to <code>MainTable</code> class. <br/>
 * Prevents dragging of the first header column ("#"). Prevents dragging of
 * unnamed (aka special) header columns. This is needed to prevent the user from
 * putting the gui table in an inconsistent state.<br/>
 * 
 * This might not be the best way to solve this problem. Overriding
 * <code>getDraggedColumn</code> produces some ugly gui dragging artifacts if a
 * user attempts to drag something before the first columns.
 * 
 * @author Daniel Waeber
 * @author Fabian Bieker
 * @since 12/2008
 */
public class PreventDraggingJTableHeader extends JTableHeader {

    public PreventDraggingJTableHeader(TableColumnModel cm) {
        super(cm);
    }

    /**
     * Overridden to prevent dragging of first column ("#") and special (unnamed)
     * columns.
     */
    @Override
    public void setDraggedColumn(TableColumn column) {

        if (column != null) {

            // prevent dragging of "#"
            if (column.getModelIndex() == 0) {
                return;
            }

            // prevent dragging of unnamed (aka special) columns
            if (isUnnamed(column)) {
                return;
            }
        }

        super.setDraggedColumn(column);
    }

    /**
     * Overridden to prevent dragging of an other column before the first
     * columns ("#" and the unnamed ones).
     * */
    @Override
    public TableColumn getDraggedColumn() {
        TableColumn column = super.getDraggedColumn();
        if (column != null) {
            preventDragBeforeIndex(this.getTable(), column.getModelIndex(),
                    getSpecialColumnsCount());
        }

        return column;
    }

    /**
     * Note: used to prevent dragging of other columns before the special
     * columns.
     * 
     * @return count of special columns
     */
    private int getSpecialColumnsCount() {
        int count = 0;
        if (Globals.prefs.getBoolean("fileColumn")) {
            count++;
        }
        if (Globals.prefs.getBoolean("pdfColumn")) {
            count++;
        }
        if (Globals.prefs.getBoolean("urlColumn")) {
            ;
            count++;
        }
        if (Globals.prefs.getBoolean("citeseerColumn")) {
            count++;
        }
        return count;
    }

    private static boolean isUnnamed(TableColumn column) {
        return column.getHeaderValue() == null
                || "".equals(column.getHeaderValue().toString());
    }

    /**
     * Transform model index <code>mColIndex</code> to a view based index and
     * prevent dragging before model index <code>toIndex</code> (inclusive).
     */
    private static void preventDragBeforeIndex(JTable table, int mColIndex,
            int toIndex) {

        for (int c = 0; c < table.getColumnCount(); c++) {

            TableColumn col = table.getColumnModel().getColumn(c);

            // found the element in the view ...
            // ... and check if it should not be dragged
            if (col.getModelIndex() == mColIndex && c <= toIndex) {
                // Util.pr("prevented! viewIndex = " + c + " modelIndex = "
                // + mColIndex + " toIndex = " + toIndex);

                // prevent dragging (move it back ...)
                table.getColumnModel().moveColumn(toIndex, toIndex + 1);
                return; // we are done now
            }

        }
    }
}
