/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.gui;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.Globals;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

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
            // in the most recent JabRef, the special columns have a one letter heading,
            // therefore, isUnnamed will always return "false"
            // to be safe, we keep this call nevertheless
            // (this is the null check for getHeaderValue())
            if (isUnnamed(column)) {
                return;
            }
            
            // prevent dragging of special field columns
            String headerValue = column.getHeaderValue().toString();
            if (headerValue.equals("P") || headerValue.equals("Q") || headerValue.equals("R")) {
            	// the letters are guessed. Don't know, where they are set in the code.
            	return;
            }
            
            // other icon columns should also not be dragged
            // note that "P" is used for "PDF" and "Priority"
            if (headerValue.equals("F") || headerValue.equals("U")) {
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
            count++;
        }
        if (Globals.prefs.getBoolean("arxivColumn")) {
            count++;
        }

        if (Globals.prefs.getBoolean("extraFileColumns")) {
            count+=Globals.prefs.getStringArray("listOfFileColumns").length;
        }
        
        // special field columns may also not be dragged
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
	        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING))
	            count++;
	        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE))
	            count++;
	        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY))
	            count++;
	        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY))
	            count++;
	        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED))
	            count++;
	        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ))
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
