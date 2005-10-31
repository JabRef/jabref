package net.sf.jabref.gui;

import net.sf.jabref.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 12, 2005
 * Time: 10:29:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class MainTable extends JTable {
    private MainTableFormat tableFormat;
    private SortedList list;
    private boolean tableColorCodes;
    private EventSelectionModel selectionModel;
    private TableComparatorChooser comparatorChooser;
    private JScrollPane pane;

    public static final int REQUIRED = 1
    ,
    OPTIONAL = 2
    ,
    OTHER = 3;

    static {
        updateRenderers();
    }

    public MainTable(TableModel tableModel, MainTableFormat tableFormat, SortedList list) {
        super(tableModel);
        this.tableFormat = tableFormat;
        this.list = list;
        tableColorCodes = Globals.prefs.getBoolean("tableColorCodesOn");
        selectionModel = new EventSelectionModel(list);
        setSelectionModel(selectionModel);
        pane = new JScrollPane(this);
        pane.getViewport().setBackground(Globals.prefs.getColor("tableBackground"));
        setGridColor(Globals.prefs.getColor("gridColor"));
        comparatorChooser = new TableComparatorChooser(this, list, true);
        final EventList selected = getSelected();

        setupComparatorChooser();
        setWidths();

    }

    public void addSelectionListener(ListEventListener listener) {
        getSelected().addListEventListener(listener);
    }

    public JScrollPane getPane() {
        return pane;
    }

    public TableCellRenderer getCellRenderer(int row, int column) {

        int score = 0;//-3;
        TableCellRenderer renderer = defRenderer;

        int status = getCellStatus(row, column);

        /*if (!panel.coloringBySearchResults ||
                nonZeroField(row, Globals.SEARCH))
            score++;
        if (!panel.coloringByGroup ||
                nonZeroField(row, Globals.GROUPSEARCH))
            score += 2;

        // Now, a grayed out renderer is for entries with -1, and
        // a very grayed out one for entries with -2
        if (score < -1)
            renderer = veryGrayedOutRenderer;
        else if (score == -1)
            renderer = grayedOutRenderer;

        else*/ if (tableColorCodes) {

        if (column == 0) {
            // Return a renderer with red background if the entry is incomplete.
            if (!isComplete(row)) {
                incRenderer.setNumber(row);
                renderer = incRenderer;
            } else {
                compRenderer.setNumber(row);
                if (isMarked(row)) {
                    renderer = markedNumberRenderer;
                    markedNumberRenderer.setNumber(row);
                } else
                    renderer = compRenderer;
            }
        } else if (status == EntryTableModel.REQUIRED)
            renderer = reqRenderer;
        else if (status == EntryTableModel.OPTIONAL)
            renderer = optRenderer;
        else if (status == EntryTableModel.BOOLEAN)
            renderer = getDefaultRenderer(Boolean.class);
    }

        // For MARKED feature:
        if ((column != 0) && isMarked(row)) {
            renderer = markedRenderer;
        }

        return renderer;

    }

    public void setWidths() {
        // Setting column widths:
        int ncWidth = Globals.prefs.getInt("numberColWidth");
        String[] widths = Globals.prefs.getStringArray("columnWidths");
        TableColumnModel cm = getColumnModel();
        cm.getColumn(0).setPreferredWidth(ncWidth);
        for (int i = 1; i < tableFormat.padleft; i++) {
            // Lock the width of icon columns.
            cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
            cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
            cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);
        }
        for (int i = tableFormat.padleft; i < getModel().getColumnCount(); i++) {
            try {
                cm.getColumn(i).setPreferredWidth(Integer.parseInt(widths[i - tableFormat.padleft]));
            } catch (Throwable ex) {
                Globals.logger("Exception while setting column widths. Choosing default.");
                cm.getColumn(i).setPreferredWidth(GUIGlobals.DEFAULT_FIELD_LENGTH);
            }

        }
    }

    public BibtexEntry getEntryAt(int row) {
        return (BibtexEntry)list.get(row);
    }

    public BibtexEntry[] getSelectedEntries() {
        final BibtexEntry[] BE_ARRAY = new BibtexEntry[0];
        return (BibtexEntry[]) getSelected().toArray(BE_ARRAY);
    }

    private void setupComparatorChooser() {
        // First column:
        java.util.List comparators = comparatorChooser.getComparatorsForColumn(0);
        comparators.clear();
        comparators.add(new FirstColumnComparator());

        // Icon columns:
        for (int i = 1; i < tableFormat.padleft; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            String[] iconField = tableFormat.getIconTypeForColumn(i);
            comparators.add(new IconComparator(iconField));
        }
        // Remaining columns:
        for (int i = tableFormat.padleft; i < tableFormat.getColumnCount(); i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            comparators.add(new FieldComparator(tableFormat.getColumnName(i).toLowerCase()));
        }

        // Set initial sort columns:

        // Default sort order:
        String[] sortFields = new String[] {Globals.prefs.get("priSort"), Globals.prefs.get("secSort"),
            Globals.prefs.get("terSort")};
        boolean[] sortDirections = new boolean[] {Globals.prefs.getBoolean("priDescending"),
            Globals.prefs.getBoolean("secDescending"), Globals.prefs.getBoolean("terDescending")}; // descending

        for (int i=0; i<sortFields.length; i++) {
            int index = tableFormat.getColumnIndex(sortFields[i]);
            if (index >= 0) {
                comparatorChooser.appendComparator(index, 0, sortDirections[i]);
            }
        }

    }

    public int getCellStatus(int row, int col) {
        try {
            BibtexEntry be = (BibtexEntry) list.get(row);
            BibtexEntryType type = be.getType();
            String columnName = tableFormat.getColumnName(col).toLowerCase();
            if (columnName.equals(GUIGlobals.KEY_FIELD) || type.isRequired(columnName)) {
                return REQUIRED;
            }
            if (type.isOptional(columnName)) {
                return OPTIONAL;
            }
            return OTHER;
        } catch (NullPointerException ex) {
            //System.out.println("Exception: getCellStatus");
            return OTHER;
        }
    }

    public EventList getSelected() {
        return selectionModel.getSelected();
    }

    public int findEntry(BibtexEntry entry) {
        return list.indexOf(entry);
    }

    public String[] getIconTypeForColumn(int column) {
        return tableFormat.getIconTypeForColumn(column);
    }

    private boolean nonZeroField(int row, String field) {
        BibtexEntry be = (BibtexEntry) list.get(row);
        Object o = be.getField(field);
        return ((o == null) || !o.equals("0"));
    }

    private boolean isComplete(int row) {
        try {
            BibtexEntry be = (BibtexEntry) list.get(row);
            return be.hasAllRequiredFields();
        } catch (NullPointerException ex) {
            //System.out.println("Exception: isComplete");
            return true;
        }
    }

    private boolean isMarked(int row) {
        try {
            BibtexEntry be = (BibtexEntry) list.get(row);
            return Util.isMarked(be);
        } catch (NullPointerException ex) {
            //System.out.println("Exception: isMarked");
            return false;
        }
    }


    public void scrollTo(int y) {
        JScrollBar scb = pane.getVerticalScrollBar();
        scb.setValue(y * scb.getUnitIncrement(1));
    }

    /**
     * updateFont
     */
    public void updateFont() {
        setFont(GUIGlobals.CURRENTFONT);
        setRowHeight(GUIGlobals.TABLE_ROW_PADDING + GUIGlobals.CURRENTFONT.getSize());
    }

    public void ensureVisible(int row) {
        JScrollBar vert = pane.getVerticalScrollBar();
        int y = row * getRowHeight();
        if ((y < vert.getValue()) || (y > vert.getValue() + vert.getVisibleAmount()))
            scrollToCenter(row, 1);
    }

    public void scrollToCenter(int rowIndex, int vColIndex) {
        if (!(this.getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) this.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = this.getCellRect(rowIndex, vColIndex, true);

        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);

        revalidate();
        repaint();
    }


    private static GeneralRenderer defRenderer
    ,
    reqRenderer
    ,
    optRenderer
    ,
    grayedOutRenderer
    ,
    veryGrayedOutRenderer
    ,
    markedRenderer;

    private static IncompleteRenderer incRenderer;
    private static CompleteRenderer compRenderer
    ,
    markedNumberRenderer;

    public static void updateRenderers() {

        boolean antialiasing = Globals.prefs.getBoolean("antialias");
        defRenderer = new GeneralRenderer(Globals.prefs.getColor("tableBackground"),
                Globals.prefs.getColor("tableText"), antialiasing);
        reqRenderer = new GeneralRenderer(Globals.prefs.getColor("tableReqFieldBackground"), Globals.prefs.getColor("tableText"), antialiasing);
        optRenderer = new GeneralRenderer(Globals.prefs.getColor("tableOptFieldBackground"), Globals.prefs.getColor("tableText"), antialiasing);
        incRenderer = new IncompleteRenderer(antialiasing);
        compRenderer = new CompleteRenderer(Globals.prefs.getColor("tableBackground"), antialiasing);
        markedNumberRenderer = new CompleteRenderer(Globals.prefs.getColor("markedEntryBackground"), antialiasing);
        grayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor("grayedOutBackground"),
                Globals.prefs.getColor("grayedOutText"), antialiasing);
        veryGrayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor("veryGrayedOutBackground"),
                Globals.prefs.getColor("veryGrayedOutText"), antialiasing);
        markedRenderer = new GeneralRenderer(Globals.prefs.getColor("markedEntryBackground"),
                Globals.prefs.getColor("tableText"), antialiasing);
    }

    static class IncompleteRenderer extends GeneralRenderer {
        public IncompleteRenderer(boolean antialiasing) {
            super(Globals.prefs.getColor("incompleteEntryBackground"), antialiasing);
            super.setToolTipText(Globals.lang("This entry is incomplete"));
        }

        protected void setNumber(int number) {
            super.setValue(String.valueOf(number + 1));
        }

        protected void setValue(Object value) {

        }
    }

    static class CompleteRenderer extends GeneralRenderer {
        public CompleteRenderer(Color color, boolean antialiasing) {
            super(color, antialiasing);
        }

        protected void setNumber(int number) {
            super.setValue(String.valueOf(number + 1));
        }

        protected void setValue(Object value) {

        }
    }

}
