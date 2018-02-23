package org.jabref.gui.maintable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.undo.UndoManager;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.EntryMarker;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.renderer.CompleteRenderer;
import org.jabref.gui.renderer.GeneralRenderer;
import org.jabref.gui.renderer.IncompleteRenderer;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.util.LocalDragboard;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import ca.odell.glazedlists.matchers.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTable extends TableView<BibEntryTableViewModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTable.class);

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
    private final BibDatabaseContext database;
    private final UndoManager undoManager;
    // needed to activate/deactivate the listener
    private PersistenceTableColumnListener tableColumnListener;

    private final MainTableDataModel model;

    public MainTable(MainTableDataModel model, JabRefFrame frame,
            BasePanel panel, BibDatabaseContext database, MainTablePreferences preferences, ExternalFileTypes externalFileTypes, KeyBindingRepository keyBindingRepository) {
        super();
        this.model = model;
        this.database = Objects.requireNonNull(database);
        this.undoManager = panel.getUndoManager();

        this.getColumns().addAll(new MainTableColumnFactory(database, preferences.getColumnPreferences(), externalFileTypes, panel.getUndoManager(), frame.getDialogService()).createColumns());
        new ViewModelTableRowFactory<BibEntryTableViewModel>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getClickCount() == 2) {
                        panel.showAndEdit(entry.getEntry());
                    }
                })
                .withContextMenu(entry1 -> RightClickMenu.create(entry1, keyBindingRepository, panel, Globals.getKeyPrefs()))
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver)
                .setOnMouseDragEntered(this::handleOnDragEntered)
                .install(this);
        if (preferences.resizeColumnsToFit()) {
            this.setColumnResizePolicy(new SmartConstrainedResizePolicy());
        }
        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        this.setItems(model.getEntriesFiltered());

        // Enable sorting
        model.bindComparator(this.comparatorProperty());

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

        setupKeyBindings(keyBindingRepository);
    }

    public void clearAndSelect(BibEntry bibEntry) {
        findEntry(bibEntry)
                .ifPresent(entry -> {
                    getSelectionModel().clearSelection();
                    getSelectionModel().select(entry);
                    scrollTo(entry);
                });
    }

    public void copy() {
        List<BibEntry> selectedEntries = getSelectedEntries();

        if (!selectedEntries.isEmpty()) {
            try {
                Globals.clipboardManager.setClipboardContent(selectedEntries);
                panel.output(panel.formatOutputMessage(Localization.lang("Copied"), selectedEntries.size()));
            } catch (IOException e) {
                LOGGER.error("Error while copying selected entries to clipboard", e);
            }
        }
    }

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

    public void cut() {
        copy();
        panel.delete(true);
    }

    private void setupKeyBindings(KeyBindingRepository keyBindings) {
        this.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindings.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case SELECT_FIRST_ENTRY:
                        clearAndSelectFirst();
                        event.consume();
                        break;
                    case SELECT_LAST_ENTRY:
                        clearAndSelectLast();
                        event.consume();
                        break;
                    case PASTE:
                        paste();
                        event.consume();
                        break;
                    case COPY:
                        copy();
                        event.consume();
                        break;
                    case CUT:
                        cut();
                        event.consume();
                        break;
                    default:
                        // Pass other keys to parent
                }
            }
        });
    }

    private void clearAndSelectFirst() {
        getSelectionModel().clearSelection();
        getSelectionModel().selectFirst();
        scrollTo(0);
    }

    private void clearAndSelectLast() {
        getSelectionModel().clearSelection();
        getSelectionModel().selectLast();
        scrollTo(getItems().size() - 1);
    }

    public void paste() {
        // Find entries in clipboard
        List<BibEntry> entriesToAdd = new ClipBoardManager().extractBibEntriesFromClipboard();

        if (!entriesToAdd.isEmpty()) {
            // Add new entries
            NamedCompound ce = new NamedCompound((entriesToAdd.size() > 1 ? Localization.lang("paste entries") : Localization.lang("paste entry")));
            for (BibEntry entryToAdd : entriesToAdd) {
                UpdateField.setAutomaticFields(entryToAdd, Globals.prefs.getUpdateFieldPreferences());

                database.getDatabase().insertEntry(entryToAdd);

                ce.addEdit(new UndoableInsertEntry(database.getDatabase(), entryToAdd));
            }
            ce.end();
            undoManager.addEdit(ce);

            panel.output(panel.formatOutputMessage(Localization.lang("Pasted"), entriesToAdd.size()));

            // Show editor if user want us to do this
            BibEntry firstNewEntry = entriesToAdd.get(0);
            if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                panel.showAndEdit(firstNewEntry);
            }

            // Select and focus first new entry
            clearAndSelect(firstNewEntry);
            this.requestFocus();
        }
    }

    private void handleOnDragOver(BibEntryTableViewModel originalItem, DragEvent event) {
        if ((event.getGestureSource() != originalItem) && LocalDragboard.INSTANCE.hasType(DragAndDropDataFormats.BIBENTRY_LIST_CLASS)) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY, TransferMode.LINK);
        }
    }

    private void handleOnDragEntered(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel entry, MouseDragEvent event) {
        // Support the following gesture to select entries: click on one row -> hold mouse button -> move over other rows
        // We need to select all items between the starting row and the row where the user currently hovers the mouse over
        // It is not enough to just select the currently hovered row since then sometimes rows are not marked selected if the user moves to fast
        TableRow<BibEntryTableViewModel> sourceRow = (TableRow<BibEntryTableViewModel>) event.getGestureSource();
        getSelectionModel().selectRange(sourceRow.getIndex(), row.getIndex());
    }

    private void handleOnDragDetected(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel entry, MouseEvent event) {
        // Start drag'n'drop
        row.startFullDrag();

        List<BibEntry> entries = getSelectionModel().getSelectedItems().stream().map(BibEntryTableViewModel::getEntry).collect(Collectors.toList());

        if (entries != null) {
            ClipboardContent content = new ClipboardContent();
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            content.put(DragAndDropDataFormats.ENTRIES, "");
            dragboard.setContent(content);
            LocalDragboard.INSTANCE.putValue(DragAndDropDataFormats.BIBENTRY_LIST_CLASS, entries);
        }

        event.consume();
    }

    private void handleOnDragDropped(BibEntryTableViewModel originalItem, DragEvent event) {

        boolean success = false;

        ObservableList<BibEntryTableViewModel> items = this.itemsProperty().get();

        if (LocalDragboard.INSTANCE.hasType(DragAndDropDataFormats.BIBENTRY_LIST_CLASS)) {
            List<BibEntry> parsedEntries = LocalDragboard.INSTANCE.getValue(DragAndDropDataFormats.BIBENTRY_LIST_CLASS);
            success = true;
        }
        if (event.getDragboard().hasContent(DataFormat.FILES)) {

            List<File> files = event.getDragboard().getFiles();
            System.out.println(files);
        }
        event.setDropCompleted(success);
        event.consume();

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
        Color sel = MainTable.defRenderer.getTableCellRendererComponent(new JTable(), "", true, false, 0, 0).getBackground();
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
