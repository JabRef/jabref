package org.jabref.gui.maintable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JTable;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.EntryMarker;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.renderer.CompleteRenderer;
import org.jabref.gui.renderer.GeneralRenderer;
import org.jabref.gui.renderer.IncompleteRenderer;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.TypedBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import ca.odell.glazedlists.matchers.Matcher;

public class MainTable extends TableView<BibEntryTableViewModel> {
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

    private final BasePanel panel;
    //private final boolean tableColorCodes;
    //private final boolean tableResolvedColorCodes;

    private final ScrollPane pane;
    // needed to activate/deactivate the listener
    private PersistenceTableColumnListener tableColumnListener;

    private final MainTableDataModel model;
    // Enum used to define how a cell should be rendered.
    private enum CellRendererMode {
        REQUIRED,
        RESOLVED,
        OPTIONAL,
        OTHER
        }

    static {
        //MainTable.updateRenderers();
    }

    public MainTable(MainTableDataModel model, JabRefFrame frame,
                     BasePanel panel, BibDatabaseContext database, MainTablePreferences preferences, ExternalFileTypes externalFileTypes, KeyBindingRepository keyBindingRepository) {
        super();
        this.model = model;

        this.getColumns().addAll(new MainTableColumnFactory(database, preferences.getColumnPreferences(), externalFileTypes, panel.getUndoManager()).createColumns());
        this.setRowFactory(new ViewModelTableRowFactory<BibEntryTableViewModel>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getClickCount() == 2) {
                        panel.showAndEdit(entry.getEntry());
                    }
                })
                .withContextMenu(entry1 -> RightClickMenu.create(entry1, keyBindingRepository, panel, Globals.getKeyPrefs())));
        if (preferences.resizeColumnsToFit()) {
            this.setColumnResizePolicy(new SmartConstrainedResizePolicy());
        }
        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ObservableList<BibEntryTableViewModel> entries = model.getEntriesFiltered();
        this.setItems(entries);

        // Enable sorting
        //entries.comparatorProperty().bind(this.comparatorProperty());

        // TODO: Cannot add focus listener as it is expecting an swing component
        //addFocusListener(Globals.getFocusListener());

        this.panel = panel;

        pane = new ScrollPane(this);
        pane.setFitToHeight(true);
        pane.setFitToWidth(true);

        this.pane.getStylesheets().add(MainTable.class.getResource("MainTable.css").toExternalForm());
        // TODO: Color
        //tableColorCodes = Globals.prefs.getBoolean(JabRefPreferences.TABLE_COLOR_CODES_ON);
        //tableResolvedColorCodes = Globals.prefs.getBoolean(JabRefPreferences.TABLE_RESOLVED_COLOR_CODES_ON);
        //pane.getViewport().setBackground(Globals.prefs.getColor(JabRefPreferences.TABLE_BACKGROUND));
        //setGridColor(Globals.prefs.getColor(JabRefPreferences.GRID_COLOR));
        if (!preferences.showGrid()) {
            this.setStyle("-fx-table-cell-border-color: transparent;");
        }

        // TODO: Tooltip for column header
        /*
        @Override
    public String getToolTipText(MouseEvent event) {
        int index = columnModel.getColumnIndexAtX(event.getX());
        int realIndex = columnModel.getColumn(index).getModelIndex();
        MainTableColumn column = tableFormat.getTableColumn(realIndex);
        return column.getDisplayName();
    }
         */

        // TODO: store column widths
        //this.tableColumnListener = new PersistenceTableColumnListener(this);
        //setWidths();

        // TODO: enable DnD
        //setDragEnabled(true);
        //TransferHandler xfer = new EntryTableTransferHandler(this, frame, panel);
        //setTransferHandler(xfer);
        //pane.setTransferHandler(xfer);

        // Todo: Set default sort order
        // setupComparatorChooser();

        // TODO: Float marked entries
        //model.updateMarkingState(Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));

        // TODO: Keybindings
        //Override 'selectNextColumnCell' and 'selectPreviousColumnCell' to move rows instead of cells on TAB
        /*
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
        });*/
    }

    public void addSelectionListener(ListChangeListener<? super BibEntryTableViewModel> listener) {
        getSelectionModel().getSelectedItems().addListener(listener);
    }

    public ScrollPane getPane() {
        return pane;
    }

    public MainTableDataModel getTableModel() {
        return model;
    }

    /*
    // TODO: if the content of the cell is bigger than the cell itself render it as the tooltip
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

    // TODO: Support float mode
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
    */

    public BibEntry getEntryAt(int row) {
        return model.getEntriesFiltered().get(row).getEntry();
    }

    public List<BibEntry> getSelectedEntries() {
        return getSelectionModel()
                .getSelectedItems().stream()
                .map(BibEntryTableViewModel::getEntry)
                .collect(Collectors.toList());
    }

    /**
     * This method sets up what Comparators are used for the various table columns.
     * The ComparatorChooser enables and disables such Comparators as the user clicks
     * columns, but this is where the Comparators are defined. Also, the ComparatorChooser
     * is initialized with the sort order defined in Preferences.
     */
    private void setupComparatorChooser() {
        // TODO: Proper sorting
        /*
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
*/
    }
/*
    // TODO: Reenable background coloring of fields (required/optional/...)
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
    */

    public Optional<BibEntryTableViewModel> findEntry(BibEntry entry) {
        return model.getEntriesFiltered().stream()
                .filter(viewModel -> viewModel.getEntry().equals(entry))
                .findFirst();
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
            return Optional.of(model.getEntriesFiltered().get(row).getEntry());
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    /**
     * Repaints the table with the most recent font configuration
     */
    public void updateFont() {
        /*
        // TODO: Font & padding customization
        setFont(GUIGlobals.currentFont);
        int maxOfIconsAndFontSize = Math.max(GUIGlobals.currentFont.getSize(), Globals.prefs.getInt(JabRefPreferences.ICON_SIZE_SMALL));
        setRowHeight(Globals.prefs.getInt(JabRefPreferences.TABLE_ROW_PADDING) + maxOfIconsAndFontSize);
        // Update Table header with new settings
        this.getTableHeader().setDefaultRenderer(new MainTableHeaderRenderer(this.getTableHeader().getDefaultRenderer()));
        this.getTableHeader().resizeAndRepaint();
        */
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
}
