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
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
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
    private final ImportHandler importHandler;
    private final CustomLocalDragboard localDragboard = GUIGlobals.localDragboard;

    public MainTable(MainTableDataModel model, JabRefFrame frame,
                     BasePanel panel, BibDatabaseContext database,
                     MainTablePreferences preferences, ExternalFileTypes externalFileTypes, KeyBindingRepository keyBindingRepository) {
        super();

        this.model = model;
        this.database = Objects.requireNonNull(database);
        this.undoManager = panel.getUndoManager();

        importHandler = new ImportHandler(
                frame.getDialogService(), database, externalFileTypes,
                Globals.prefs.getFilePreferences(),
                Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getUpdateFieldPreferences(),
                Globals.getFileUpdateMonitor(),
                undoManager);

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

        /*for (Entry<String, SortType> entries : preferences.getColumnPreferences().getSortTypesForColumns().entrySet()) {
            Optional<TableColumn<BibEntryTableViewModel, ?>> column = this.getColumns().stream().filter(col -> entries.getKey().equals(col.getText())).findFirst();
            column.ifPresent(col -> {
                col.setSortType(entries.getValue());
                this.getSortOrder().add(col);
            });
        }*/

        if (preferences.resizeColumnsToFit()) {
            this.setColumnResizePolicy(new SmartConstrainedResizePolicy());
        }
        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        this.setItems(model.getEntriesFilteredAndSorted());
        // Enable sorting
        model.getEntriesFilteredAndSorted().comparatorProperty().bind(this.comparatorProperty());

        this.panel = panel;

        pane = new ScrollPane(this);
        pane.setFitToHeight(true);
        pane.setFitToWidth(true);

        this.pane.getStylesheets().add(MainTable.class.getResource("MainTable.css").toExternalForm());

        // Store visual state
        new PersistenceVisualStateTable(this, Globals.prefs);

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
                Globals.clipboardManager.setContent(selectedEntries);
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
        List<BibEntry> entriesToAdd = Globals.clipboardManager.extractEntries();

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
        if ((event.getGestureSource() != originalItem) && localDragboard.hasType(DragAndDropDataFormats.BIBENTRY_LIST_CLASS)) {
            event.acceptTransferModes(TransferMode.MOVE);

        }
        if (event.getDragboard().hasFiles() && (event.getSource() instanceof TableRow)) {
            event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
        }
        event.consume(); //need to consume it here to stop the DnDTabPane from getting the event

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

        //The following is necesary to initiate the drag and drop in javafx, although we don't need the contents
        //It doesn't work without
        ClipboardContent content = new ClipboardContent();
        Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
        content.put(DragAndDropDataFormats.ENTRIES, "");
        dragboard.setContent(content);

        if (!entries.isEmpty()) {
            localDragboard.putBibEntries(entries);
        }

        event.consume();
    }

    private void handleOnDragDropped(BibEntryTableViewModel originalItem, DragEvent event) {
        boolean success = false;

        if (event.getDragboard().hasContent(DataFormat.FILES)) {
            List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());
            List<Path> bibFiles = files.stream().filter(FileUtil::isBibFile).collect(Collectors.toList());

            if (!bibFiles.isEmpty()) {
                // Import all bibtex entries contained in the dropped bib files
                for (Path file : bibFiles) {
                    importHandler.importEntriesFromBibFiles(file);
                }
                success = true;
            }
            if (event.getGestureTarget() instanceof TableRow) {
                // Depending on the pressed modifier, import as new entries or link to drop target
                BibEntry entry = originalItem.getEntry();
                if ((event.getTransferMode() == TransferMode.MOVE)) {
                    LOGGER.debug("Mode MOVE"); //shift on win or no modifier
                    importHandler.importAsNewEntries(files);
                    success = true;
                }

                if (event.getTransferMode() == TransferMode.LINK) {
                    LOGGER.debug("LINK"); //alt on win
                    importHandler.getLinker().moveFilesToFileDirAndAddToEntry(entry, files);
                    success = true;
                }

                if (event.getTransferMode() == TransferMode.COPY) {
                    LOGGER.debug("Mode Copy"); //ctrl on win
                    importHandler.getLinker().copyFilesToFileDirAndAddToEntry(entry, files);
                    success = true;
                }
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
