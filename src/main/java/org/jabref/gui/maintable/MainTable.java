package org.jabref.gui.maintable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.ListChangeListener;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiles.NewDroppedFileHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.util.LocalDragboard;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTable extends TableView<BibEntryTableViewModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTable.class);

    private final BasePanel panel;

    private final ScrollPane pane;
    private final BibDatabaseContext database;
    private final UndoManager undoManager;

    private final MainTableDataModel model;
    private final NewDroppedFileHandler fileHandler;

    public MainTable(MainTableDataModel model, JabRefFrame frame,
                     BasePanel panel, BibDatabaseContext database,
                     MainTablePreferences preferences, ExternalFileTypes externalFileTypes, KeyBindingRepository keyBindingRepository) {
        super();

        this.model = model;
        this.database = Objects.requireNonNull(database);
        this.undoManager = panel.getUndoManager();

        fileHandler = new NewDroppedFileHandler(frame.getDialogService(), database, externalFileTypes, Globals.prefs.getFileDirectoryPreferences(), Globals.prefs.getCleanupPreferences(Globals.journalAbbreviationLoader).getFileDirPattern(), Globals.prefs.getImportFormatPreferences());

        this.getColumns().addAll(new MainTableColumnFactory(database, preferences.getColumnPreferences(), externalFileTypes, panel.getUndoManager(), frame.getDialogService()).createColumns());
        new ViewModelTableRowFactory<BibEntryTableViewModel>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getClickCount() == 2) {
                        panel.showAndEdit(entry.getEntry());
                    }
                })
                .withContextMenu(entry -> RightClickMenu.create(entry, keyBindingRepository, panel, Globals.getKeyPrefs(), frame.getDialogService()))
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver)
                .setOnMouseDragEntered(this::handleOnDragEntered)
                .install(this);

        if (preferences.resizeColumnsToFit()) {
            this.setColumnResizePolicy(new SmartConstrainedResizePolicy());
        }
        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        this.setItems(model.getEntriesFilteredAndSorted());

        // Enable sorting
        model.bindComparator(this.comparatorProperty());

        this.panel = panel;

        pane = new ScrollPane(this);
        pane.setFitToHeight(true);
        pane.setFitToWidth(true);

        this.pane.getStylesheets().add(MainTable.class.getResource("MainTable.css").toExternalForm());
        
        // Store visual state
        new PersistenceVisualStateTable(this, Globals.prefs);

        // TODO: enable DnD
        //setDragEnabled(true);
        //TransferHandler xfer = new EntryTableTransferHandler(this, frame, panel);
        //setTransferHandler(xfer);
        //pane.setTransferHandler(xfer);

        // TODO: Float marked entries
        //model.updateMarkingState(Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));

        setupKeyBindings(keyBindingRepository);
    }

    public void clearAndSelect(BibEntry bibEntry) {
        findEntry(bibEntry).ifPresent(entry -> {
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

    public void cut() {
        copy();
        panel.delete(true);
    }

    private void setupKeyBindings(KeyBindingRepository keyBindings) {
        this.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                getSelectedEntries().stream()
                                    .findFirst()
                                    .ifPresent(panel::showAndEdit);
                event.consume();
                return;
            }

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
            event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
        }
    }

    private void handleOnDragEntered(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel entry, MouseDragEvent event) {
        // Support the following gesture to select entries: click on one row -> hold mouse button -> move over other rows
        // We need to select all items between the starting row and the row where the user currently hovers the mouse over
        // It is not enough to just select the currently hovered row since then sometimes rows are not marked selected if the user moves to fast
        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
        TableRow<BibEntryTableViewModel> targetRow = (TableRow<BibEntryTableViewModel>) event.getGestureTarget();

        BibEntry entry = this.getItems().get(targetRow.getIndex()).getEntry();

        if (event.getDragboard().hasContent(DataFormat.FILES)) {
            List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());
            System.out.println(files);

            if (event.getTransferMode() == TransferMode.MOVE) {

                System.out.println("Mode MOVE"); //shift on win or no modifier
                fileHandler.addNewEntryFromXMPorPDFContent(entry, files);
                success = true;
            }
            if (event.getTransferMode() == TransferMode.LINK) {
                System.out.println("LINK"); //alt on win
                fileHandler.addToEntryAndMoveToFileDir(entry, files);
                success = true;

            }
            if (event.getTransferMode() == TransferMode.COPY) {
                System.out.println("Mode Copy");

            }
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

    public BibEntry getEntryAt(int row) {
        return model.getEntriesFilteredAndSorted().get(row).getEntry();
    }

    public List<BibEntry> getSelectedEntries() {
        return getSelectionModel()
                .getSelectedItems()
                .stream()
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

    private Optional<BibEntryTableViewModel> findEntry(BibEntry entry) {
        return model.getEntriesFilteredAndSorted()
                .stream()
                .filter(viewModel -> viewModel.getEntry().equals(entry))
                .findFirst();
    }

    /**
     * Repaints the table with the most recent font configuration
     */
    public void updateFont() {
        // TODO: Font & padding customization
        // setFont(GUIGlobals.currentFont);
    }
}
