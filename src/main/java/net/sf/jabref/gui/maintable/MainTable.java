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

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexSingleField;
import net.sf.jabref.bibtex.comparator.FieldComparator;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.groups.GroupMatcher;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.EntryMarker;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.renderer.CompleteRenderer;
import net.sf.jabref.gui.renderer.GeneralRenderer;
import net.sf.jabref.gui.renderer.IncompleteRenderer;
import net.sf.jabref.gui.search.matchers.SearchMatcher;
import net.sf.jabref.gui.util.comparator.FirstColumnComparator;
import net.sf.jabref.gui.util.comparator.IconComparator;
import net.sf.jabref.gui.util.comparator.RankingFieldComparator;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The central table which displays the bibtex entries.
 *
 * User: alver
 * Date: Oct 12, 2005
 * Time: 10:29:39 PM
 *
 */
public class MainTable extends JTable {



    private static final Log LOGGER = LogFactory.getLog(MainTable.class);

    private final MainTableFormat tableFormat;
    private final BasePanel panel;

    private final boolean tableColorCodes;
    private final DefaultEventSelectionModel<BibEntry> localSelectionModel;
    private final TableComparatorChooser<BibEntry> comparatorChooser;
    private final JScrollPane pane;

    // needed to activate/deactivate the listener
    private final PersistenceTableColumnListener tableColumnListener;
    private final MainTableDataModel model;

    public MainTableDataModel getTableModel() {
        return model;
    }

    // Enum used to define how a cell should be rendered.
    private enum CellRendererMode {
        REQUIRED,
        OPTIONAL,
        OTHER
    }
    private static GeneralRenderer defRenderer;
    private static GeneralRenderer reqRenderer;
    private static GeneralRenderer optRenderer;
    private static GeneralRenderer grayedOutRenderer;
    private static GeneralRenderer veryGrayedOutRenderer;

    private static List<GeneralRenderer> markedRenderers;

    private static IncompleteRenderer incRenderer;
    private static CompleteRenderer compRenderer;
    private static CompleteRenderer grayedOutNumberRenderer;
    private static CompleteRenderer veryGrayedOutNumberRenderer;

    private static List<CompleteRenderer> markedNumberRenderers;


    static {
        MainTable.updateRenderers();
    }


    public MainTable(MainTableFormat tableFormat, MainTableDataModel model, JabRefFrame frame,
            BasePanel panel) {
        super();
        this.model = model;

        addFocusListener(Globals.focusListener);
        setAutoResizeMode(Globals.prefs.getInt(JabRefPreferences.AUTO_RESIZE_MODE));

        this.tableFormat = tableFormat;
        this.panel = panel;


        setModel(GlazedListsSwing
                .eventTableModelWithThreadProxyList(model.getTableRows(), tableFormat));

        tableColorCodes = Globals.prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON);
        localSelectionModel = (DefaultEventSelectionModel<BibEntry>) GlazedListsSwing
                .eventSelectionModelWithThreadProxyList(model.getTableRows());
        setSelectionModel(localSelectionModel);
        pane = new JScrollPane(this);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(Globals.prefs.getColor(JabRefPreferences.TABLE_BACKGROUND));
        setGridColor(Globals.prefs.getColor(JabRefPreferences.GRID_COLOR));
        if (Globals.prefs.getBoolean(JabRefPreferences.TABLE_SHOW_GRID)) {
            setShowGrid(true);
        } else {
            setShowGrid(false);
            setIntercellSpacing(new Dimension(0, 0));
        }

        this.setTableHeader(new PreventDraggingJTableHeader(this, tableFormat));

        comparatorChooser = this.createTableComparatorChooser(this, model.getSortedForUserDefinedTableColumnSorting(),
                TableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);

        this.tableColumnListener = new PersistenceTableColumnListener(this);

        // set table header render AFTER creation of comparatorChooser (this enables sort arrow rendering)
        this.getTableHeader().setDefaultRenderer(new MainTableHeaderRenderer(this.getTableHeader().getDefaultRenderer()));

        // TODO: Figure out, whether this call is needed.
        getSelected();

        // enable DnD
        setDragEnabled(true);
        TransferHandler xfer = new EntryTableTransferHandler(this, frame, panel);
        setTransferHandler(xfer);
        pane.setTransferHandler(xfer);

        setupComparatorChooser();
        model.updateMarkingState(Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));
        setWidths();
    }

    public void addSelectionListener(ListEventListener<BibEntry> listener) {
        getSelected().addListEventListener(listener);
    }

    public JScrollPane getPane() {
        return pane;
    }

    @Override
    public String getToolTipText(MouseEvent e) {

        // Set tooltip text for all columns which are not fully displayed

        String toolTipText = null;
        Point p = e.getPoint();
        int col = columnAtPoint(p);
        int row = rowAtPoint(p);
        Component comp = prepareRenderer(getCellRenderer(row, col), row, col);

        Rectangle bounds = getCellRect(row, col, false);

        Dimension d = comp.getPreferredSize();
        if ((d != null) && (d.width > bounds.width) && (getValueAt(row, col) != null)) {
            toolTipText = getValueAt(row, col).toString();
        }
        return toolTipText;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {

        int score = -3;
        DefaultTableCellRenderer renderer = MainTable.defRenderer;

        CellRendererMode status = getCellStatus(row, column);

        if (!(model.getSearchState() == MainTableDataModel.DisplayOption.FLOAT) || matches(row, model.getSearchState() != MainTableDataModel.DisplayOption.DISABLED ? SearchMatcher.INSTANCE : null)) {
            score++;
        }
        if (!(model.getGroupingState() == MainTableDataModel.DisplayOption.FLOAT) || matches(row, model.getGroupingState() != MainTableDataModel.DisplayOption.DISABLED ? GroupMatcher.INSTANCE : null)) {
            score += 2;
        }

        // Now, a grayed out renderer is for entries with -1, and
        // a very grayed out one for entries with -2
        if (score < -1) {
            if (column == 0) {
                MainTable.veryGrayedOutNumberRenderer.setNumber(row);
                renderer = MainTable.veryGrayedOutNumberRenderer;
            } else {
                renderer = MainTable.veryGrayedOutRenderer;
            }
        }
        else if (score == -1) {
            if (column == 0) {
                MainTable.grayedOutNumberRenderer.setNumber(row);
                renderer = MainTable.grayedOutNumberRenderer;
            } else {
                renderer = MainTable.grayedOutRenderer;
            }
        }

        else if (column == 0) {
            if (isComplete(row)) {
                MainTable.compRenderer.setNumber(row);
                int marking = isMarked(row);
                if (marking > 0) {
                    marking = Math.min(marking, EntryMarker.MARK_COLOR_LEVELS);
                    renderer = MainTable.markedNumberRenderers.get(marking - 1);
                    MainTable.markedNumberRenderers.get(marking - 1).setNumber(row);
                } else {
                    renderer = MainTable.compRenderer;
                }
            } else {
                // Return a renderer with red background if the entry is incomplete.
                MainTable.incRenderer.setNumber(row);
                renderer = MainTable.incRenderer;
            }
            renderer.setHorizontalAlignment(JLabel.CENTER);
        } else if (tableColorCodes) {
            if (status == CellRendererMode.REQUIRED) {
                renderer = MainTable.reqRenderer;
            } else if (status == CellRendererMode.OPTIONAL) {
                renderer = MainTable.optRenderer;
            }
        }

        // For MARKED feature:
        int marking = isMarked(row);
        if ((column != 0) && (marking > 0)) {
            marking = Math.min(marking, EntryMarker.MARK_COLOR_LEVELS);
            renderer = MainTable.markedRenderers.get(marking - 1);
        }

        return renderer;

    }

    private void setWidths() {
        // Setting column widths:
        int ncWidth = Globals.prefs.getInt(JabRefPreferences.NUMBER_COL_WIDTH);
        List<String> widthsFromPreferences = Globals.prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS);
        TableColumnModel cm = getColumnModel();
        cm.getColumn(0).setPreferredWidth(ncWidth);
        for (int i = 1; i < cm.getColumnCount(); i++) {
            MainTableColumn mainTableColumn = tableFormat.getTableColumn(cm.getColumn(i).getModelIndex());
            if (SpecialFieldsUtils.FIELDNAME_RANKING.equals(mainTableColumn.getColumnName())) {
                cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
                cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
                cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL_RANKING);
            } else if (mainTableColumn.isIconColumn()) {
                cm.getColumn(i).setPreferredWidth(GUIGlobals.WIDTH_ICON_COL);
                cm.getColumn(i).setMinWidth(GUIGlobals.WIDTH_ICON_COL);
                cm.getColumn(i).setMaxWidth(GUIGlobals.WIDTH_ICON_COL);
            } else {
                List<String> allColumns = Globals.prefs.getStringList(JabRefPreferences.COLUMN_NAMES);
                // find index of current mainTableColumn in allColumns
                for (int j = 0; j < allColumns.size(); j++) {
                    if (allColumns.get(j).equalsIgnoreCase(mainTableColumn.getDisplayName())) {
                        try {
                            // set preferred width by using found index j in the width array
                            cm.getColumn(i).setPreferredWidth(Integer.parseInt(widthsFromPreferences.get(j)));
                        } catch (NumberFormatException e) {
                            LOGGER.info("Exception while setting column widths. Choosing default.", e);
                            cm.getColumn(i).setPreferredWidth(BibtexSingleField.DEFAULT_FIELD_LENGTH);
                        }
                        break;
                    }
                }
            }
        }
    }

    public BibEntry getEntryAt(int row) {
        return model.getTableRows().get(row);
    }

    /**
     * @return the return value is never null
     */
    public List<BibEntry> getSelectedEntries() {
        return new ArrayList<>(getSelected());
    }

    private List<Boolean> getCurrentSortOrder() {
        List<Boolean> order = new ArrayList<>();
        List<Integer> sortCols = comparatorChooser.getSortingColumns();
        for (Integer i : sortCols) {
            order.add(comparatorChooser.isColumnReverse(i));
        }
        return order;
    }

    private List<String> getCurrentSortFields() {
        List<Integer> sortCols = comparatorChooser.getSortingColumns();
        List<String> fields = new ArrayList<>();
        for (Integer i : sortCols) {
            // TODO check whether this really works
            String name = tableFormat.getColumnName(i);
            //TODO OLD
            // String name = tableFormat.getColumnType(i);
            if (name != null) {
                fields.add(name.toLowerCase());
            }
        }
        return fields;
    }

    /**
     * This method sets up what Comparators are used for the various table columns.
     * The ComparatorChooser enables and disables such Comparators as the user clicks
     * columns, but this is where the Comparators are defined. Also, the ComparatorChooser
     * is initialized with the sort order defined in Preferences.
     */
    private void setupComparatorChooser() {
        // First column:
        List<Comparator> comparators = comparatorChooser.getComparatorsForColumn(0);
        comparators.clear();
        comparators.add(new FirstColumnComparator(panel.getBibDatabaseContext()));

        for (int i = 1; i < tableFormat.getColumnCount(); i++) {
            MainTableColumn tableColumn = tableFormat.getTableColumn(i);

            comparators = comparatorChooser.getComparatorsForColumn(i);
            comparators.clear();

            if (SpecialFieldsUtils.FIELDNAME_RANKING.equals(tableColumn.getColumnName())) {
                comparators.add(new RankingFieldComparator());
            } else if (tableColumn.isIconColumn()) {
                comparators.add(new IconComparator(tableColumn.getBibtexFields()));
            } else {
                comparators = comparatorChooser.getComparatorsForColumn(i);
                comparators.clear();
                comparators.add(new FieldComparator(tableFormat.getColumnName(i).toLowerCase()));
            }
        }

        // Set initial sort columns:

        // Default sort order:
        String[] sortFields = new String[] {
                Globals.prefs.get(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD),
                Globals.prefs.get(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD),
                Globals.prefs.get(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD)
        };
        boolean[] sortDirections = new boolean[] {
                Globals.prefs.getBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING),
                Globals.prefs.getBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING),
                Globals.prefs.getBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING)
        }; // descending

        model.getSortedForUserDefinedTableColumnSorting().getReadWriteLock().writeLock().lock();
        try {
            for (int i = 0; i < sortFields.length; i++) {
                int index = -1;

                // TODO where is this prefix set?
//                if (!sortFields[i].startsWith(MainTableFormat.ICON_COLUMN_PREFIX))
                if (sortFields[i].startsWith("iconcol:")) {
                    for (int j = 0; j < tableFormat.getColumnCount(); j++) {
                        if (sortFields[i].equals(tableFormat.getColumnName(j))) {
                            index = j;
                            break;
                        }
                    }
                } else {
                    index = tableFormat.getColumnIndex(sortFields[i]);
                }
                if (index >= 0) {
                    comparatorChooser.appendComparator(index, 0, sortDirections[i]);
                }
            }
        } finally {
            model.getSortedForUserDefinedTableColumnSorting().getReadWriteLock().writeLock().unlock();
        }

        // Add action listener so we can remember the sort order:
        comparatorChooser.addSortActionListener(e -> {
            // Get the information about the current sort order:
            List<String> fields = getCurrentSortFields();
            List<Boolean> order = getCurrentSortOrder();
            // Update preferences:
            int count = Math.min(fields.size(), order.size());
            if (count >= 1) {
                Globals.prefs.put(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD, fields.get(0));
                Globals.prefs.putBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING, order.get(0));
            }
            if (count >= 2) {
                Globals.prefs.put(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD, fields.get(1));
                Globals.prefs.putBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING, order.get(1));
            } else {
                Globals.prefs.put(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD, "");
                Globals.prefs.putBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING, false);
            }
            if (count >= 3) {
                Globals.prefs.put(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD, fields.get(2));
                Globals.prefs.putBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING, order.get(2));
            } else {
                Globals.prefs.put(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD, "");
                Globals.prefs.putBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING, false);
            }
        });

    }

    private CellRendererMode getCellStatus(int row, int col) {
        try {
            BibEntry be = getEntryAt(row);
            Optional<EntryType> type = EntryTypes.getType(be.getType(), panel.getBibDatabaseContext().getMode());
            if(type.isPresent()) {
                String columnName = getColumnName(col).toLowerCase();
                if (columnName.equals(BibEntry.KEY_FIELD) || type.get().getRequiredFieldsFlat().contains(columnName)) {
                    return CellRendererMode.REQUIRED;
                }
                if (type.get().getOptionalFields().contains(columnName)) {
                    return CellRendererMode.OPTIONAL;
                }
            }
            return CellRendererMode.OTHER;
        } catch (NullPointerException ex) {
            return CellRendererMode.OTHER;
        }
    }

    /**
     * Use with caution! If you modify an entry in the table, the selection changes
     *
     * You can avoid it with
     *   <code>.getSelected().getReadWriteLock().writeLock().lock()</code>
     *   and then <code>.unlock()</code>
     */
    public EventList<BibEntry> getSelected() {
        return localSelectionModel.getSelected();
    }

    /**
     * Selects the given row
     *
     * @param row the row to select
     */
    public void setSelected(int row) {
        localSelectionModel.setSelectionInterval(row, row);
    }

    public int findEntry(BibEntry entry) {
        return model.getTableRows().indexOf(entry);
    }

    /**
     * method to check whether a MainTableColumn at the modelIndex refers to the file field (either as a specific
     * file extension filter or not)
     *
     * @param modelIndex model index of the column to check
     * @return true if the column shows the "file" field; false otherwise
     */
    public boolean isFileColumn(int modelIndex) {
        return (tableFormat.getTableColumn(modelIndex) != null) && tableFormat.getTableColumn(modelIndex)
                .getBibtexFields().contains(Globals.FILE_FIELD);
    }

    private boolean matches(int row, Matcher<BibEntry> m) {
        return m.matches(getBibEntry(row));
    }

    private boolean isComplete(int row) {
        try {
            BibEntry entry = getBibEntry(row);
            TypedBibEntry typedEntry = new TypedBibEntry(entry, Optional.of(panel.getDatabase()), panel.getBibDatabaseContext().getMode());
            return typedEntry.hasAllRequiredFields();
        } catch (NullPointerException ex) {
            return true;
        }
    }

    private int isMarked(int row) {
        try {
            BibEntry be = getBibEntry(row);
            return EntryMarker.isMarked(be);
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    private BibEntry getBibEntry(int row) {
        return model.getTableRows().get(row);
    }

    public void scrollTo(int y) {
        JScrollBar scb = pane.getVerticalScrollBar();
        scb.setValue(y * scb.getUnitIncrement(1));
    }

    public void showFloatSearch() {
        this.getTableModel().updateSearchState(MainTableDataModel.DisplayOption.FLOAT);

        scrollTo(0);
    }

    /**
     * updateFont
     */
    public void updateFont() {
        setFont(GUIGlobals.currentFont);
        setRowHeight(Globals.prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING) + GUIGlobals.currentFont.getSize());
    }

    public void ensureVisible(int row) {
        JScrollBar vert = pane.getVerticalScrollBar();
        int y = row * getRowHeight();
        if ((y < vert.getValue()) || ((y > (vert.getValue() + vert.getVisibleAmount())) && !(model.getSearchState() == MainTableDataModel.DisplayOption.FLOAT))) {
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



    public static void updateRenderers() {

        MainTable.defRenderer = new GeneralRenderer(Globals.prefs.getColor(JabRefPreferences.TABLE_BACKGROUND),
                Globals.prefs.getColor(JabRefPreferences.TABLE_TEXT));
        Color sel = MainTable.defRenderer.getTableCellRendererComponent
                (new JTable(), "", true, false, 0, 0).getBackground();
        MainTable.reqRenderer = new GeneralRenderer(Globals.prefs.getColor(JabRefPreferences.TABLE_REQ_FIELD_BACKGROUND), Globals.prefs.getColor(JabRefPreferences.TABLE_TEXT));
        MainTable.optRenderer = new GeneralRenderer(Globals.prefs.getColor(JabRefPreferences.TABLE_OPT_FIELD_BACKGROUND), Globals.prefs.getColor(JabRefPreferences.TABLE_TEXT));
        MainTable.incRenderer = new IncompleteRenderer();
        MainTable.compRenderer = new CompleteRenderer(Globals.prefs.getColor(JabRefPreferences.TABLE_BACKGROUND));
        MainTable.grayedOutNumberRenderer = new CompleteRenderer(Globals.prefs.getColor(JabRefPreferences.GRAYED_OUT_BACKGROUND));
        MainTable.veryGrayedOutNumberRenderer = new CompleteRenderer(Globals.prefs.getColor(JabRefPreferences.VERY_GRAYED_OUT_BACKGROUND));
        MainTable.grayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor(JabRefPreferences.GRAYED_OUT_BACKGROUND),
                Globals.prefs.getColor(JabRefPreferences.GRAYED_OUT_TEXT), MainTable.mixColors(Globals.prefs.getColor(JabRefPreferences.GRAYED_OUT_BACKGROUND),
                        sel));
        MainTable.veryGrayedOutRenderer = new GeneralRenderer(Globals.prefs.getColor(JabRefPreferences.VERY_GRAYED_OUT_BACKGROUND),
                Globals.prefs.getColor(JabRefPreferences.VERY_GRAYED_OUT_TEXT), MainTable.mixColors(Globals.prefs.getColor(JabRefPreferences.VERY_GRAYED_OUT_BACKGROUND),
                        sel));

        MainTable.markedRenderers = new ArrayList<>(EntryMarker.MARK_COLOR_LEVELS);
        MainTable.markedNumberRenderers = new ArrayList<>(EntryMarker.MARK_COLOR_LEVELS);
        for (int i = 0; i < EntryMarker.MARK_COLOR_LEVELS; i++) {
            Color c = Globals.prefs.getColor(JabRefPreferences.MARKED_ENTRY_BACKGROUND + i);
            MainTable.markedRenderers.add(new GeneralRenderer(c, Globals.prefs.getColor(JabRefPreferences.TABLE_TEXT),
                    MainTable.mixColors(Globals.prefs.getColor(JabRefPreferences.MARKED_ENTRY_BACKGROUND + i), sel)));
            MainTable.markedNumberRenderers.add(new CompleteRenderer(c));
        }

    }

    private static Color mixColors(Color one, Color two) {
        return new Color((one.getRed() + two.getRed()) / 2, (one.getGreen() + two.getGreen()) / 2,
                (one.getBlue() + two.getBlue()) / 2);
    }

    private TableComparatorChooser<BibEntry> createTableComparatorChooser(JTable table, SortedList<BibEntry> list,
                                                                             Object sortingStrategy) {
        return TableComparatorChooser.install(table, list, sortingStrategy);
    }

    /**
     * Morten Alver: This override is a workaround NullPointerException when
     * dragging stuff into the table. I found this in a forum, but have no idea
     * why it works.
     * @param newUI
     */
    @Override
    public void setUI(TableUI newUI) {
        super.setUI(newUI);
        TransferHandler handler = getTransferHandler();
        setTransferHandler(null);
        setTransferHandler(handler);

    }

    /**
     * Find out which column is set as sort column.
     * @param number The position in the sort hierarchy (primary, secondary, etc.)
     * @return The sort column number.
     */
    public int getSortingColumn(int number) {
        List<Integer> l = comparatorChooser.getSortingColumns();
        if (l.size() <= number) {
            return -1;
        } else {
            return l.get(number);
        }
    }

    public PersistenceTableColumnListener getTableColumnListener() {
        return tableColumnListener;
    }

    public MainTableColumn getMainTableColumn(int modelIndex) {
        return tableFormat.getTableColumn(modelIndex);
    }
}
