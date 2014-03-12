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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.*;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.search.HitOrMissComparator;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;

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
    private BasePanel panel;
    private SortedList<BibtexEntry> sortedForMarking, sortedForTable, sortedForSearch, sortedForGrouping;
    private boolean tableColorCodes, showingFloatSearch=false, showingFloatGrouping=false;
    private EventSelectionModel<BibtexEntry> selectionModel;
    private TableComparatorChooser<BibtexEntry> comparatorChooser;
    private JScrollPane pane;
    private Comparator<BibtexEntry> searchComparator, groupComparator,
            markingComparator = new IsMarkedComparator();
    private Matcher<BibtexEntry> searchMatcher, groupMatcher;
    
    // needed to activate/deactivate the listener
    private final PersistenceTableColumnListener tableColumnListener;

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


    public MainTable(MainTableFormat tableFormat, EventList<BibtexEntry> list, JabRefFrame frame,
                     BasePanel panel) {
        super();

        addFocusListener(Globals.focusListener);
        setAutoResizeMode(Globals.prefs.getInt("autoResizeMode"));
        this.tableFormat = tableFormat;
        this.panel = panel;
        // This SortedList has a Comparator controlled by the TableComparatorChooser
        // we are going to install, which responds to user sorting selctions:
        sortedForTable = new SortedList<BibtexEntry>(list, null);
        // This SortedList applies afterwards, and floats marked entries:
        sortedForMarking = new SortedList<BibtexEntry>(sortedForTable, null);
        // This SortedList applies afterwards, and can float search hits:
        sortedForSearch = new SortedList<BibtexEntry>(sortedForMarking, null);
        // This SortedList applies afterwards, and can float grouping hits:
        sortedForGrouping = new SortedList<BibtexEntry>(sortedForSearch, null);


        searchMatcher = null;
        groupMatcher = null;
        searchComparator = null;//new HitOrMissComparator(searchMatcher);
        groupComparator = null;//new HitOrMissComparator(groupMatcher);

        EventTableModel<BibtexEntry> tableModel = new EventTableModel<BibtexEntry>(sortedForGrouping, tableFormat);
        setModel(tableModel);

        tableColorCodes = Globals.prefs.getBoolean("tableColorCodesOn");
        selectionModel = new EventSelectionModel<BibtexEntry>(sortedForGrouping);
        setSelectionModel(selectionModel);
        pane = new JScrollPane(this);
        pane.getViewport().setBackground(Globals.prefs.getColor("tableBackground"));
        setGridColor(Globals.prefs.getColor("gridColor"));

        this.setTableHeader(new PreventDraggingJTableHeader(this.getColumnModel()));

        comparatorChooser = new MyTableComparatorChooser(this, sortedForTable,
                TableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);

        this.tableColumnListener =  new PersistenceTableColumnListener(this);
        /*if (Globals.prefs.getBoolean(PersistenceTableColumnListener.ACTIVATE_PREF_KEY)) {
            getColumnModel().addColumnModelListener(this.tableColumnListener );
        }*/

        // TODO: Figure out, whether this call is needed.
        getSelected();

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
    public void showFloatSearch(Matcher<BibtexEntry> m) {
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
    public void showFloatGrouping(Matcher<BibtexEntry> m) {
        showingFloatGrouping = true;
        groupMatcher = m;
        groupComparator = new HitOrMissComparator(m);
        refreshSorting();
    }


    public boolean isShowingFloatSearch() {
        return showingFloatSearch;
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

    public EventList<BibtexEntry> getTableRows() {
        return sortedForGrouping;
    }
    public void addSelectionListener(ListEventListener<BibtexEntry> listener) {
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
                int marking = isMarked(row);
                if (marking > 0) {
                    marking = Math.min(marking, Util.MARK_COLOR_LEVELS);
                    renderer = markedNumberRenderers[marking-1];
                    markedNumberRenderers[marking-1].setNumber(row);
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
        int marking = isMarked(row);
        if ((column != 0) && (marking > 0)) {
            marking = Math.min(marking, Util.MARK_COLOR_LEVELS);
            renderer = markedRenderers[marking-1];
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
        	
        	// Check if the Column is an extended RankingColumn (and not a compact-ranking column) 
        	// If this is the case, set a certain Column-width,
        	// because the RankingIconColumn needs some more width
        	if (tableFormat.isRankingColumn(i) && !Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_RANKING_COMPACT)) {
        		// Lock the width of ranking icon column.
                cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
                cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
                cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
        	} else {
        		// Lock the width of icon columns.
                cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
                cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
                cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);	
        	}
        	
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
        return sortedForGrouping.get(row);
    }

    public BibtexEntry[] getSelectedEntries() {
        final BibtexEntry[] BE_ARRAY = new BibtexEntry[0];
        return getSelected().toArray(BE_ARRAY);
    }

    public List<Boolean> getCurrentSortOrder() {
        List<Boolean> order = new ArrayList<Boolean>();
        List<Integer> sortCols = comparatorChooser.getSortingColumns();
        for (Iterator<Integer> iterator = sortCols.iterator(); iterator.hasNext();) {
            int i = iterator.next();
            order.add(comparatorChooser.isColumnReverse(i));
        }
        return order;
    }

    public List<String> getCurrentSortFields() {
        List<Integer> sortCols = comparatorChooser.getSortingColumns();
        List<String> fields = new ArrayList<String>();
        for (Iterator<Integer> iterator = sortCols.iterator(); iterator.hasNext();) {
            int i =  iterator.next();
            String name = tableFormat.getColumnType(i);
            if (name != null)
                fields.add(name.toLowerCase());
        }
        return fields;
    }


    /**
     * This method sets up what Comparators are used for the various table columns.
     * The ComparatorChooser enables and disables such Comparators as the user clicks
     * columns, but this is where the Comparators are defined. Also, the ComparatorChooser
     * is initialized with the sort order defined in Preferences.
     */
    @SuppressWarnings("unchecked")
	private void setupComparatorChooser() {
        // First column:
        List<Comparator> comparators = comparatorChooser.getComparatorsForColumn(0);
        comparators.clear();
        comparators.add(new FirstColumnComparator(panel.database()));

        // Icon columns:
        for (int i = 1; i < tableFormat.padleft; i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            String[] iconField = tableFormat.getIconTypeForColumn(i);
            
            if (iconField[0].equals(SpecialFieldsUtils.FIELDNAME_RANKING)) {
            	comparators.add(new RankingFieldComparator());
            } else {
            	comparators.add(new IconComparator(iconField));
            }
        }
        // Remaining columns:
        for (int i = tableFormat.padleft; i < tableFormat.getColumnCount(); i++) {
            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();
            comparators.add(new FieldComparator(tableFormat.getColumnName(i).toLowerCase()));
        }

        // Set initial sort columns:

        // Default sort order:
        String[] sortFields = new String[] {
            Globals.prefs.get(JabRefPreferences.PRIMARY_SORT_FIELD), 
            Globals.prefs.get(JabRefPreferences.SECONDARY_SORT_FIELD),
            Globals.prefs.get(JabRefPreferences.TERTIARY_SORT_FIELD)
        };
        boolean[] sortDirections = new boolean[]{
            Globals.prefs.getBoolean(JabRefPreferences.PRIMARY_SORT_DESCENDING),
            Globals.prefs.getBoolean(JabRefPreferences.SECONDARY_SORT_DESCENDING),
            Globals.prefs.getBoolean(JabRefPreferences.TERTIARY_SORT_DESCENDING)
        }; // descending

        sortedForTable.getReadWriteLock().writeLock().lock();
        for (int i=0; i<sortFields.length; i++) {
            int index = -1;
            if (!sortFields[i].startsWith(MainTableFormat.ICON_COLUMN_PREFIX))
                index = tableFormat.getColumnIndex(sortFields[i]);
            else {
                for (int j=0; j<tableFormat.getColumnCount(); j++) {
                    if (sortFields[i].equals(tableFormat.getColumnType(j))) {
                        index = j;
                        break;
                    }
                }
            }
            if (index >= 0) {
                comparatorChooser.appendComparator(index, 0, sortDirections[i]);
            }
        }
        sortedForTable.getReadWriteLock().writeLock().unlock();

        // Add action listener so we can remember the sort order:
        comparatorChooser.addSortActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                // Get the information about the current sort order:
                List<String> fields = getCurrentSortFields(); 
                List<Boolean> order = getCurrentSortOrder();
                // Update preferences:
                int count = Math.min(fields.size(), order.size());
                if (count >= 1) {
                    Globals.prefs.put(JabRefPreferences.PRIMARY_SORT_FIELD, fields.get(0));
                    Globals.prefs.putBoolean(JabRefPreferences.PRIMARY_SORT_DESCENDING, order.get(0));
                }
                if (count >= 2) {
                    Globals.prefs.put(JabRefPreferences.SECONDARY_SORT_FIELD, fields.get(1));
                    Globals.prefs.putBoolean(JabRefPreferences.SECONDARY_SORT_DESCENDING, order.get(1));
                }
                else {
                    Globals.prefs.put(JabRefPreferences.SECONDARY_SORT_FIELD, "");
                    Globals.prefs.putBoolean(JabRefPreferences.SECONDARY_SORT_DESCENDING, false);
                }
                if (count >= 3) {
                    Globals.prefs.put(JabRefPreferences.TERTIARY_SORT_FIELD, fields.get(2));
                    Globals.prefs.putBoolean(JabRefPreferences.TERTIARY_SORT_DESCENDING, order.get(2));
                }
                else {
                    Globals.prefs.put(JabRefPreferences.TERTIARY_SORT_FIELD, "");
                    Globals.prefs.putBoolean(JabRefPreferences.TERTIARY_SORT_DESCENDING, false);
                }
            }

        });


    }

    public int getCellStatus(int row, int col) {
        try {
            BibtexEntry be = sortedForGrouping.get(row);
            BibtexEntryType type = be.getType();
            String columnName = getColumnName(col).toLowerCase();
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

    public EventList<BibtexEntry> getSelected() {
        return selectionModel.getSelected();
    }

    /**
     * Selects the given row
     *
     * @param row the row to select
     */
    public void setSelected(int row) {
    	selectionModel.setSelectionInterval(row, row);
    }

    /**
     * Adds the given row to the selection
     * @param row the row to add to the selection
     */
    public void addSelection(int row)  {
    	this.selectionModel.addSelectionInterval(row, row);
    }

    public int findEntry(BibtexEntry entry) {
        //System.out.println(sortedForGrouping.indexOf(entry));
        return sortedForGrouping.indexOf(entry);
    }

    public String[] getIconTypeForColumn(int column) {
        return tableFormat.getIconTypeForColumn(column);
    }

    private boolean matches(int row, Matcher<BibtexEntry> m) {
        return m.matches(sortedForGrouping.get(row));
    }

    private boolean isComplete(int row) {
        try {
            BibtexEntry be = sortedForGrouping.get(row);
            return be.hasAllRequiredFields(panel.database());
        } catch (NullPointerException ex) {
            //System.out.println("Exception: isComplete");
            return true;
        }
    }

    private int isMarked(int row) {
        try {
            BibtexEntry be = sortedForGrouping.get(row);
            return Util.isMarked(be);
        } catch (NullPointerException ex) {
            //System.out.println("Exception: isMarked");
            return 0;
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
		if ((y < vert.getValue()) || (y > vert.getValue() + vert.getVisibleAmount()) && !showingFloatSearch) {
			scrollToCenter(row, 1);
		}
            
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


    private static GeneralRenderer defRenderer, reqRenderer, optRenderer, grayedOutRenderer,
        veryGrayedOutRenderer;

    private static GeneralRenderer[] markedRenderers;

    private static IncompleteRenderer incRenderer;
    private static CompleteRenderer
            compRenderer,
            grayedOutNumberRenderer,
            veryGrayedOutNumberRenderer;

    private static CompleteRenderer[] markedNumberRenderers;

    public static void updateRenderers() {

        defRenderer = new GeneralRenderer(Globals.prefs.getColor("tableBackground"),
                Globals.prefs.getColor("tableText"));
        Color sel = defRenderer.getTableCellRendererComponent
                (new JTable(), "", true, false, 0, 0).getBackground();
        reqRenderer = new GeneralRenderer(Globals.prefs.getColor("tableReqFieldBackground"), Globals.prefs.getColor("tableText"));
        optRenderer = new GeneralRenderer(Globals.prefs.getColor("tableOptFieldBackground"), Globals.prefs.getColor("tableText"));
        incRenderer = new IncompleteRenderer();
        compRenderer = new CompleteRenderer(Globals.prefs.getColor("tableBackground"));
        grayedOutNumberRenderer = new CompleteRenderer(Globals.prefs.getColor("grayedOutBackground"));
        veryGrayedOutNumberRenderer = new CompleteRenderer(Globals.prefs.getColor("veryGrayedOutBackground"));
        grayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor("grayedOutBackground"),
            Globals.prefs.getColor("grayedOutText"), mixColors(Globals.prefs.getColor("grayedOutBackground"),
                sel));
        veryGrayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor("veryGrayedOutBackground"),
                Globals.prefs.getColor("veryGrayedOutText"), mixColors(Globals.prefs.getColor("veryGrayedOutBackground"),
                sel));

        markedRenderers = new GeneralRenderer[Util.MARK_COLOR_LEVELS];
        markedNumberRenderers = new CompleteRenderer[Util.MARK_COLOR_LEVELS];
        for (int i=0; i<Util.MARK_COLOR_LEVELS; i++) {
            Color c = Globals.prefs.getColor("markedEntryBackground"+i);
            markedRenderers[i] = new GeneralRenderer(c,
                Globals.prefs.getColor("tableText"), mixColors(Globals.prefs.getColor("markedEntryBackground"+i), sel));
            markedNumberRenderers[i] = new CompleteRenderer(c);
        }
        
    }

    private static Color mixColors(Color one, Color two) {
        return new Color((one.getRed()+two.getRed())/2, (one.getGreen()+two.getGreen())/2,
                (one.getBlue()+two.getBlue())/2);
    }

    static class IncompleteRenderer extends GeneralRenderer {
        public IncompleteRenderer() {
            super(Globals.prefs.getColor("incompleteEntryBackground"));
            super.setToolTipText(Globals.lang("This entry is incomplete"));
        }

        protected void setNumber(int number) {
            super.setValue(String.valueOf(number + 1));
        }

        protected void setValue(Object value) {

        }
    }

    static class CompleteRenderer extends GeneralRenderer {
        public CompleteRenderer(Color color) {
            super(color);
        }

        protected void setNumber(int number) {
            super.setValue(String.valueOf(number + 1));
        }

        protected void setValue(Object value) {

        }
    }

    class MyTableComparatorChooser extends TableComparatorChooser<BibtexEntry> {
        public MyTableComparatorChooser(JTable table, SortedList<BibtexEntry> list,
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
    @SuppressWarnings("unchecked")
	public Comparator<BibtexEntry> getComparatorForColumn(int index) {
        List<Comparator> l = comparatorChooser.getComparatorsForColumn(index);
        return l.size() == 0 ? null : l.get(0);
    }

    /**
     * Find out which column is set as sort column.
     * @param number The position in the sort hierarchy (primary, secondary, etc.)
     * @return The sort column number.
     */
    public int getSortingColumn(int number) {
        List<Integer> l = comparatorChooser.getSortingColumns();
        if (l.size() <= number)
            return -1;
        else
            return (l.get(number)).intValue();
    }
    
    public PersistenceTableColumnListener getTableColumnListener() {
		return tableColumnListener; 
	}


    /**
     * Returns the List of entries sorted by a user-selected term. This is the
     * sorting before marking, search etc. applies.
     *
     * Note: The returned List must not be modified from the outside
     * @return The sorted list of entries.
     */
    public SortedList<BibtexEntry> getSortedForTable() {
        return sortedForTable;
    }
}
