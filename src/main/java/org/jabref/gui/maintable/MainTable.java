package org.jabref.gui.maintable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.TransferHandler;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.EntryMarker;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.groups.EntryTableTransferHandler;
import org.jabref.gui.groups.GroupMatcher;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.renderer.CompleteRenderer;
import org.jabref.gui.renderer.GeneralRenderer;
import org.jabref.gui.renderer.IncompleteRenderer;
import org.jabref.gui.search.matchers.SearchMatcher;
import org.jabref.gui.util.comparator.FirstColumnComparator;
import org.jabref.gui.util.comparator.IconComparator;
import org.jabref.gui.util.comparator.RankingFieldComparator;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.model.EntryTypes;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexSingleField;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.preferences.JabRefPreferences;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTable extends JTable {
    private static GeneralRenderer defRenderer;
    private static GeneralRenderer reqRenderer;
    private static GeneralRenderer optRenderer;
    private static GeneralRenderer resolvedRenderer;
    private static GeneralRenderer grayedOutRenderer;
    private static GeneralRenderer veryGrayedOutRenderer;

    private static List<GeneralRenderer> markedRenderers;

    private static IncompleteRenderer incRenderer;
    private static CompleteRenderer compRenderer;
    private static CompleteRenderer grayedOutNumberRenderer;
    private static CompleteRenderer veryGrayedOutNumberRenderer;

    private static List<CompleteRenderer> markedNumberRenderers;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTable.class);
    private final MainTableFormat tableFormat;

    private final BasePanel panel;
    private final boolean tableColorCodes;
    private final boolean tableResolvedColorCodes;
    private final DefaultEventSelectionModel<BibEntry> localSelectionModel;
    private final TableComparatorChooser<BibEntry> comparatorChooser;

    private final JScrollPane pane;
    // needed to activate/deactivate the listener
    private final PersistenceTableColumnListener tableColumnListener;

    private final MainTableDataModel model;
    // Enum used to define how a cell should be rendered.
    private enum CellRendererMode {
        REQUIRED,
        RESOLVED,
        OPTIONAL,
        OTHER
        }

    static {
        MainTable.updateRenderers();
    }

    public MainTable(MainTableFormat tableFormat, MainTableDataModel model, JabRefFrame frame,
            BasePanel panel) {
        super();
        this.model = model;

        addFocusListener(Globals.getFocusListener());
        setAutoResizeMode(Globals.prefs.getInt(JabRefPreferences.AUTO_RESIZE_MODE));

        this.tableFormat = tableFormat;
        this.panel = panel;

        setModel(GlazedListsSwing
                .eventTableModelWithThreadProxyList(model.getTableRows(), tableFormat));

        tableColorCodes = Globals.prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON);
        tableResolvedColorCodes = Globals.prefs.getBoolean(JabRefPreferences.TABLE_RESOLVED_COLOR_CODES_ON);
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
                AbstractTableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);

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

        //Override 'selectNextColumnCell' and 'selectPreviousColumnCell' to move rows instead of cells on TAB
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap();
        actionMap.put("selectNextColumnCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.selectNextEntry();
            }
        });
        actionMap.put("selectPreviousColumnCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.selectPreviousEntry();
            }
        });
        actionMap.put("selectNextRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.selectNextEntry();
            }
        });
        actionMap.put("selectPreviousRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.selectPreviousEntry();
            }
        });

        String selectFirst = "selectFirst";
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.SELECT_FIRST_ENTRY), selectFirst);
        actionMap.put(selectFirst, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                panel.selectFirstEntry();
            }
        });

        String selectLast = "selectLast";
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.SELECT_LAST_ENTRY), selectLast);
        actionMap.put(selectLast, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                panel.selectLastEntry();
            }
        });
    }

    public void addSelectionListener(ListEventListener<BibEntry> listener) {
        getSelected().addListEventListener(listener);
    }

    public JScrollPane getPane() {
        return pane;
    }

    public MainTableDataModel getTableModel() {
        return model;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        String toolTipText = super.getToolTipText(e);
        Point p = e.getPoint();
        int col = columnAtPoint(p);
        int row = rowAtPoint(p);

        Rectangle bounds = getCellRect(row, col, false);
        Dimension d = prepareRenderer(getCellRenderer(row, col), row, col).getPreferredSize();
        // if the content of the cell is bigger than the cell itself render it as the tooltip (thus throwing the original tooltip away)
        if ((d != null) && (d.width > bounds.width) && (getValueAt(row, col) != null)) {
            toolTipText = getValueAt(row, col).toString();
        }
        return toolTipText;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {

        int score = -3;
        DefaultTableCellRenderer renderer = MainTable.defRenderer;

        if ((model.getSearchState() != MainTableDataModel.DisplayOption.FLOAT)
                || matches(row, SearchMatcher.INSTANCE)) {
            score++;
        }
        if ((model.getGroupingState() != MainTableDataModel.DisplayOption.FLOAT)
                || matches(row, GroupMatcher.INSTANCE)) {
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
        } else if (tableColorCodes || tableResolvedColorCodes) {
            CellRendererMode status = getCellStatus(row, column, tableResolvedColorCodes);
            if (status == CellRendererMode.REQUIRED) {
                renderer = MainTable.reqRenderer;
            } else if (status == CellRendererMode.OPTIONAL) {
                renderer = MainTable.optRenderer;
            } else if (status == CellRendererMode.RESOLVED) {
                renderer = MainTable.resolvedRenderer;
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
            if (SpecialField.RANKING.getFieldName().equals(mainTableColumn.getColumnName())) {
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
                fields.add(name.toLowerCase(Locale.ROOT));
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

            if (SpecialField.RANKING.getFieldName().equals(tableColumn.getColumnName())) {
                comparators.add(new RankingFieldComparator());
            } else if (tableColumn.isIconColumn()) {
                comparators.add(new IconComparator(tableColumn.getBibtexFields()));
            } else {
                comparators = comparatorChooser.getComparatorsForColumn(i);
                comparators.clear();
                comparators.add(new FieldComparator(tableFormat.getColumnName(i).toLowerCase(Locale.ROOT)));
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

    private CellRendererMode getCellStatus(int row, int col, boolean checkResolved) {
        try {
            BibEntry be = getEntryAt(row);
            if (checkResolved && tableFormat.getTableColumn(col).isResolved(be)) {
                return CellRendererMode.RESOLVED;
            }
            Optional<EntryType> type = EntryTypes.getType(be.getType(), panel.getBibDatabaseContext().getMode());
            if (type.isPresent()) {
                String columnName = getColumnName(col).toLowerCase(Locale.ROOT);
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
        EventList<BibEntry> tableRows = model.getTableRows();
        for (int row = 0; row < tableRows.size(); row++) {
            BibEntry bibEntry = tableRows.get(row);
            if (entry == bibEntry) { // NOPMD (equals doesn't recognise duplicates)
                return row;
            }
        }
        return -1;
    }

    /**
     * Method to check whether a MainTableColumn at the modelIndex refers to the file field (either as a specific
     * file extension filter or not)
     *
     * @param modelIndex model index of the column to check
     * @return true if the column shows the "file" field; false otherwise
     */
    public boolean isFileColumn(int modelIndex) {
        return (tableFormat.getTableColumn(modelIndex) != null) && tableFormat.getTableColumn(modelIndex)
                .getBibtexFields().contains(FieldName.FILE);
    }

    private boolean matches(int row, Matcher<BibEntry> m) {
        return getBibEntry(row).map(m::matches).orElse(false);
    }

    private boolean isComplete(int row) {
        Optional<BibEntry> bibEntry = getBibEntry(row);

        if (bibEntry.isPresent()) {
            TypedBibEntry typedEntry = new TypedBibEntry(bibEntry.get(), panel.getBibDatabaseContext());
            return typedEntry.hasAllRequiredFields();
        }
        return true;
    }

    private int isMarked(int row) {
        Optional<BibEntry> bibEntry = getBibEntry(row);

        if (bibEntry.isPresent()) {
            return EntryMarker.isMarked(bibEntry.get());
        }
        return 0;
    }

    private Optional<BibEntry> getBibEntry(int row) {
        try {
            return Optional.of(model.getTableRows().get(row));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
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
     * Repaints the table with the most recent font configuration
     */
    public void updateFont() {
        setFont(GUIGlobals.currentFont);
        int maxOfIconsAndFontSize = Math.max(GUIGlobals.currentFont.getSize(), Globals.prefs.getInt(JabRefPreferences.ICON_SIZE_SMALL));
        setRowHeight(Globals.prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING) + maxOfIconsAndFontSize);
        // Update Table header with new settings
        this.getTableHeader().setDefaultRenderer(new MainTableHeaderRenderer(this.getTableHeader().getDefaultRenderer()));
        this.getTableHeader().resizeAndRepaint();
    }

    public void ensureVisible(int row) {
        JScrollBar vert = pane.getVerticalScrollBar();
        int y = row * getRowHeight();
        if ((y < vert.getValue()) || ((y >= (vert.getValue() + vert.getVisibleAmount()))
                && (model.getSearchState() != MainTableDataModel.DisplayOption.FLOAT))) {
            scrollToCenter(row, 1);
        }
    }

    /**
     * Ensures that the given entry is shown in the maintable.
     * It also selects the given entry
     * The execution is executed directly. Be sure that it happens in the EDT.
     *
     * @param entry the BibEntry to be shown
     */
    public void ensureVisible(BibEntry entry) {
        final int row = this.findEntry(entry);
        if (row >= 0) {
            if (this.getSelectedRowCount() == 0) {
                this.setRowSelectionInterval(row, row);
            }
            this.ensureVisible(row);
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
        MainTable.resolvedRenderer = new GeneralRenderer(
                Globals.prefs.getColor(JabRefPreferences.TABLE_RESOLVED_FIELD_BACKGROUND),
                Globals.prefs.getColor(JabRefPreferences.TABLE_TEXT));
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
        return new Color((one.getRed() + two.getRed()) / 2, (one.getGreen() + two.getGreen()) / 2, (one.getBlue() + two.getBlue()) / 2);
    }

    private TableComparatorChooser<BibEntry> createTableComparatorChooser(JTable table, SortedList<BibEntry> list, Object sortingStrategy) {
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

    public MainTableColumn getMainTableColumn(int modelIndex) {
        return tableFormat.getTableColumn(modelIndex);
    }
}
