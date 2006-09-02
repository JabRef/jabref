package net.sf.jabref.gui;

import net.sf.jabref.*;
import net.sf.jabref.search.SearchMatcher;
import net.sf.jabref.search.HitOrMissComparator;
import net.sf.jabref.groups.EntryTableTransferHandler;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import ca.odell.glazedlists.swing.EventTableModel;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Comparator;

/**
 * The central table which displays the bibtex entries.
 * 
 * User: alver
 * Date: Oct 12, 2005
 * Time: 10:29:39 PM
 * 
 */
public class MainTable extends JTable {
	
    private MainTableFormat tableFormat;
    private SortedList sortedForMarking, sortedForTable, sortedForSearch, sortedForGrouping;
    private boolean tableColorCodes, showingFloatSearch=false, showingFloatGrouping=false;
    private EventSelectionModel selectionModel;
    private TableComparatorChooser comparatorChooser;
    private JScrollPane pane;
    private Comparator searchComparator, groupComparator,
            markingComparator = new IsMarkedComparator();
    private Matcher searchMatcher, groupMatcher;

    // Constants used to define how a cell should be rendered.
    public static final int REQUIRED = 1, OPTIONAL = 2,
      REQ_STRING = 1,
      REQ_NUMBER = 2,
      OPT_STRING = 3,
      OTHER = 3,
      BOOLEAN = 4,
      ICON_COL = 8; // Constant to indicate that an icon cell renderer should be used.

    static {
        updateRenderers();
    }


    public MainTable(MainTableFormat tableFormat, EventList list, JabRefFrame frame,
                     BasePanel panel) {
        super();
        this.tableFormat = tableFormat;
        // This SortedList has a Comparator controlled by the TableComparatorChooser
        // we are going to install, which responds to user sorting selctions:
        sortedForTable = new SortedList(list, null);
        // This SortedList applies afterwards, and floats marked entries:
        sortedForMarking = new SortedList(sortedForTable, null);
        // This SortedList applies afterwards, and can float search hits:
        sortedForSearch = new SortedList(sortedForMarking, null);
        // This SortedList applies afterwards, and can float grouping hits:
        sortedForGrouping = new SortedList(sortedForSearch, null);


        searchMatcher = null;
        groupMatcher = null;
        searchComparator = null;//new HitOrMissComparator(searchMatcher);
        groupComparator = null;//new HitOrMissComparator(groupMatcher);

        EventTableModel tableModel = new EventTableModel(sortedForGrouping, tableFormat);
        setModel(tableModel);

        tableColorCodes = Globals.prefs.getBoolean("tableColorCodesOn");
        selectionModel = new EventSelectionModel(sortedForGrouping);
        setSelectionModel(selectionModel);
        pane = new JScrollPane(this);
        pane.getViewport().setBackground(Globals.prefs.getColor("tableBackground"));
        setGridColor(Globals.prefs.getColor("gridColor"));
        comparatorChooser = new MyTableComparatorChooser(this, sortedForTable,
                TableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);
        
        final EventList selected = getSelected();

        // enable DnD
        setDragEnabled(true);
        TransferHandler xfer = new EntryTableTransferHandler(this, frame, panel);
        setTransferHandler(xfer);
        pane.setTransferHandler(xfer);

        setupComparatorChooser();
        refreshSorting();
        setWidths();

    }

    public void refreshSorting() {
        sortedForMarking.getReadWriteLock().writeLock().lock();
        if (Globals.prefs.getBoolean("floatMarkedEntries"))
            sortedForMarking.setComparator(markingComparator);
        else
            sortedForMarking.setComparator(null);
        sortedForMarking.getReadWriteLock().writeLock().unlock();
        sortedForSearch.getReadWriteLock().writeLock().lock();
        sortedForSearch.setComparator(searchComparator);
        sortedForSearch.getReadWriteLock().writeLock().unlock();
        sortedForGrouping.getReadWriteLock().writeLock().lock();
        sortedForGrouping.setComparator(groupComparator);
        sortedForGrouping.getReadWriteLock().writeLock().unlock();
    }

    /**
     * Adds a sorting rule that floats hits to the top, and causes non-hits to be grayed out:
     * @param m The Matcher that determines if an entry is a hit or not.
     */
    public void showFloatSearch(Matcher m) {
        showingFloatSearch = true;
        searchMatcher = m;
        searchComparator = new HitOrMissComparator(m);
        refreshSorting();
        scrollTo(0);
    }

    /**
     * Removes sorting by search results, and graying out of non-hits.
     */
    public void stopShowingFloatSearch() {
        showingFloatSearch = false;
        searchMatcher = null;
        searchComparator = null;
        refreshSorting();
    }

    /**
     * Adds a sorting rule that floats group hits to the top, and causes non-hits to be grayed out:
     * @param m The Matcher that determines if an entry is a in the current group selection or not.
     */
    public void showFloatGrouping(Matcher m) {
        showingFloatGrouping = true;
        groupMatcher = m;
        groupComparator = new HitOrMissComparator(m);
        refreshSorting();
    }

    /**
     * Removes sorting by group, and graying out of non-hits.
     */
    public void stopShowingFloatGrouping() {
        showingFloatGrouping = false;
        groupMatcher = null;
        groupComparator = null;
        refreshSorting();
    }

    public EventList getTableRows() {
        return sortedForGrouping;
    }
    public void addSelectionListener(ListEventListener listener) {
        getSelected().addListEventListener(listener);
    }

    public JScrollPane getPane() {
        return pane;
    }

    public TableCellRenderer getCellRenderer(int row, int column) {

        int score = -3;
        TableCellRenderer renderer = defRenderer;

        int status = getCellStatus(row, column);

        if (!showingFloatSearch || matches(row, searchMatcher))
            score++;
        if (!showingFloatGrouping || matches(row, groupMatcher))
            score += 2;

        // Now, a grayed out renderer is for entries with -1, and
        // a very grayed out one for entries with -2
        if (score < -1) {
            if (column == 0) {
                veryGrayedOutNumberRenderer.setNumber(row);
                renderer = veryGrayedOutNumberRenderer;
            } else renderer = veryGrayedOutRenderer;
        }
        else if (score == -1) {
            if (column == 0) {
                grayedOutNumberRenderer.setNumber(row);
                renderer = grayedOutNumberRenderer;
            } else renderer = grayedOutRenderer;
        }

        else if (column == 0) {
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
        }
        else if (tableColorCodes) {
            if (status == REQUIRED)
                renderer = reqRenderer;
            else if (status == OPTIONAL)
                renderer = optRenderer;
            else if (status == BOOLEAN)
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
        return (BibtexEntry)sortedForGrouping.get(row);
    }

    public BibtexEntry[] getSelectedEntries() {
        final BibtexEntry[] BE_ARRAY = new BibtexEntry[0];
        return (BibtexEntry[]) getSelected().toArray(BE_ARRAY);
    }

    /**
     * This method sets up what Comparators are used for the various table columns.
     * The ComparatorChooser enables and disables such Comparators as the user clicks
     * columns, but this is where the Comparators are defined. Also, the ComparatorChooser
     * is initialized with the sort order defined in Preferences.
     */
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

        sortedForTable.getReadWriteLock().writeLock().lock();
        for (int i=0; i<sortFields.length; i++) {
            int index = tableFormat.getColumnIndex(sortFields[i]);
            if (index >= 0) {
                comparatorChooser.appendComparator(index, 0, sortDirections[i]);
            }
        }
        sortedForTable.getReadWriteLock().writeLock().unlock();

    }

    public int getCellStatus(int row, int col) {
        try {
            BibtexEntry be = (BibtexEntry)sortedForGrouping.get(row);
            BibtexEntryType type = be.getType();
            String columnName = tableFormat.getColumnName(col).toLowerCase();
            if (columnName.equals(BibtexFields.KEY_FIELD) || type.isRequired(columnName)) {
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
        //System.out.println(sortedForGrouping.indexOf(entry));
        return sortedForGrouping.indexOf(entry);
    }

    public String[] getIconTypeForColumn(int column) {
        return tableFormat.getIconTypeForColumn(column);
    }

    private boolean nonZeroField(int row, String field) {
        BibtexEntry be = (BibtexEntry)sortedForGrouping.get(row);
        Object o = be.getField(field);
        return ((o == null) || !o.equals("0"));
    }

    private boolean matches(int row, Matcher m) {
        Object o = sortedForGrouping.get(row);
        return m.matches(o);
    }

    private boolean isComplete(int row) {
        try {
            BibtexEntry be = (BibtexEntry)sortedForGrouping.get(row);
            return be.hasAllRequiredFields();
        } catch (NullPointerException ex) {
            //System.out.println("Exception: isComplete");
            return true;
        }
    }

    private boolean isMarked(int row) {
        try {
            BibtexEntry be = (BibtexEntry)sortedForGrouping.get(row);
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
    grayedOutRenderer,
    veryGrayedOutRenderer
    ,
    markedRenderer;

    private static IncompleteRenderer incRenderer;
    private static CompleteRenderer
            compRenderer,
            grayedOutNumberRenderer,
            veryGrayedOutNumberRenderer,
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
        grayedOutNumberRenderer = new CompleteRenderer(Globals.prefs.getColor("grayedOutBackground"), antialiasing);
        veryGrayedOutNumberRenderer = new CompleteRenderer(Globals.prefs.getColor("veryGrayedOutBackground"), antialiasing);
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

    class MyTableComparatorChooser extends TableComparatorChooser {
        public MyTableComparatorChooser(JTable table, SortedList list,
                                        Object sortingStrategy) {
            super(table, list, sortingStrategy);
            // We need to reset the stack of sorted list each time sorting order
            // changes, or the sorting breaks down:
            addSortActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //System.out.println("...");
                    refreshSorting();
                }
            });
        }
/*
        protected void columnClicked(int i, int i1) {

            super.columnClicked(i, i1);
            refreshSorting();
        }*/
    }

    /**
     * Morten Alver: This override is a workaround NullPointerException when
     * dragging stuff into the table. I found this in a forum, but have no idea
     * why it works.
     * @param newUI
     */
    public void setUI(TableUI newUI) {
        super.setUI(newUI);
        TransferHandler handler = getTransferHandler();
        setTransferHandler(null);
        setTransferHandler(handler);

    }

    /**
     * Get the first comparator set up for the given column.
     * @param index The column number.
     * @return The Comparator, or null if none is set.
     */
    public Comparator getComparatorForColumn(int index) {
        java.util.List l = comparatorChooser.getComparatorsForColumn(index);
        return l.size() == 0 ? null : (Comparator)l.get(0);
    }

    /**
     * Find out which column is set as sort column.
     * @param number The position in the sort hierarchy (primary, secondary, etc.)
     * @return The sort column number.
     */
    public int getSortingColumn(int number) {
        java.util.List l = comparatorChooser.getSortingColumns();
        if (l.size() <= number)
            return -1;
        else
            return ((Integer)l.get(number)).intValue();
    }

    /**
     * Returns the List of entries sorted by a user-selected term. This is the
     * sorting before marking, search etc. applies.
     *
     * Note: The returned List must not be modified from the outside
     * @return The sorted list of entries.
     */
    public SortedList getSortedForTable() {
        return sortedForTable;
    }
}
