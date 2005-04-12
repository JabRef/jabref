package net.sf.jabref.gui.components;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.*;

public abstract class DnDJTable extends JTable implements DragGestureListener,
        DragSourceListener {

    private int[] selectionToDrag = new int[0];

    public DnDJTable() {
        super();
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public DnDJTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public DnDJTable(TableModel dm) {
        super(dm);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public DnDJTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public DnDJTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public DnDJTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public DnDJTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, this);
    }

    public void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            // prevent selection changes due to movement
            clearSelection();
            for (int i = 0; i < selectionToDrag.length; ++i)
                addRowSelectionInterval(selectionToDrag[i], selectionToDrag[i]);
        }
    }

    public void processMouseEvent(MouseEvent e) {
        // unmodified left click on selected cell has to be handled specially
        if (e.getButton() == MouseEvent.BUTTON1 && !e.isAltDown()
                && !e.isAltGraphDown() && !e.isControlDown() && !e.isMetaDown()
                && !e.isShiftDown()) {
            Point p = e.getPoint();
            final int col = columnAtPoint(p);
            final int row = rowAtPoint(p);
            if (isCellSelected(row, col)) {
                // on button release, select only this row
                if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                    getSelectionModel().setSelectionInterval(row, row);
                    return;
                }

                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    int[] selectedRows = getSelectedRows();
                    super.processMouseEvent(e);
                    clearSelection();
                    for (int i = 0; i < selectedRows.length; ++i)
                        addRowSelectionInterval(selectedRows[i],
                                selectedRows[i]);
                    return;
                }
            }
        }
        super.processMouseEvent(e);
        // set selection here to ensure it's up to date when dragging starts
        selectionToDrag = getSelectedRows();
    }

     public abstract void dragGestureRecognized(DragGestureEvent dge);
    /* public void dragGestureRecognized(DragGestureEvent dge) {
        Transferable transferable = new TransferableObject(selectionToDrag);
        Cursor cursor = DragSource.DefaultCopyDrop;
        dge.getDragSource().startDrag(dge, cursor, transferable, this);
    }*/
    
    public void dragEnter(DragSourceDragEvent dsde) {
        // ignore
    }

    public void dragOver(DragSourceDragEvent dsde) {
        // ignore
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
        // ignore
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
        // JZTODO
    }

    public void dragExit(DragSourceEvent dse) {
        // ignore
    }
}
