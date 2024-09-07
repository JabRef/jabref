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
import javafx.css.PseudoClass;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.columns.LibraryColumn;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.search.MatchCategory;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
public class MainTable extends TableView<BibEntryTableViewModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTable.class);
    private static final PseudoClass MATCHING_SEARCH_AND_GROUPS = PseudoClass.getPseudoClass("matching-search-and-groups");
    private static final PseudoClass MATCHING_SEARCH_NOT_GROUPS = PseudoClass.getPseudoClass("matching-search-not-groups");
    private static final PseudoClass MATCHING_GROUPS_NOT_SEARCH = PseudoClass.getPseudoClass("matching-groups-not-search");
    private static final PseudoClass NOT_MATCHING_SEARCH_AND_GROUPS = PseudoClass.getPseudoClass("not-matching-search-and-groups");

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final BibDatabaseContext database;
    private final MainTableDataModel model;

    private final ImportHandler importHandler;
    private final CustomLocalDragboard localDragboard;
    private final ClipBoardManager clipBoardManager;
    private final BibEntryTypesManager entryTypesManager;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;
    private final FilePreferences filePreferences;
    private long lastKeyPressTime;
    private String columnSearchTerm;

    public MainTable(MainTableDataModel model,
                     LibraryTab libraryTab,
                     LibraryTabContainer tabContainer,
                     BibDatabaseContext database,
                     PreferencesService preferencesService,
                     DialogService dialogService,
                     StateManager stateManager,
                     KeyBindingRepository keyBindingRepository,
                     ClipBoardManager clipBoardManager,
                     BibEntryTypesManager entryTypesManager,
                     TaskExecutor taskExecutor,
                     FileUpdateMonitor fileUpdateMonitor) {
        super();

        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.database = Objects.requireNonNull(database);
        this.model = model;
        this.clipBoardManager = clipBoardManager;
        this.entryTypesManager = entryTypesManager;
        this.taskExecutor = taskExecutor;
        this.undoManager = libraryTab.getUndoManager();
        this.filePreferences = preferencesService.getFilePreferences();

        MainTablePreferences mainTablePreferences = preferencesService.getMainTablePreferences();

        importHandler = new ImportHandler(
                database,
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);

        localDragboard = stateManager.getLocalDragboard();

        this.setOnDragOver(this::handleOnDragOverTableView);
        this.setOnDragDropped(this::handleOnDragDroppedTableView);

        MainTableColumnFactory mainTableColumnFactory = new MainTableColumnFactory(
                database,
                preferencesService,
                preferencesService.getMainTableColumnPreferences(),
                undoManager,
                dialogService,
                stateManager,
                taskExecutor);

        this.getColumns().addAll(mainTableColumnFactory.createColumns());
        this.getColumns().removeIf(LibraryColumn.class::isInstance);

        new ViewModelTableRowFactory<BibEntryTableViewModel>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getClickCount() == 2) {
                        libraryTab.showAndEdit(entry.getEntry());
                    }
                })
                .withContextMenu(entry -> RightClickMenu.create(entry,
                        keyBindingRepository,
                        libraryTab,
                        dialogService,
                        stateManager,
                        preferencesService,
                        undoManager,
                        clipBoardManager,
                        taskExecutor,
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                        entryTypesManager))
                .withPseudoClass(MATCHING_SEARCH_AND_GROUPS, entry -> entry.matchCategory().isEqualTo(MatchCategory.MATCHING_SEARCH_AND_GROUPS))
                .withPseudoClass(MATCHING_SEARCH_NOT_GROUPS, entry -> entry.matchCategory().isEqualTo(MatchCategory.MATCHING_SEARCH_NOT_GROUPS))
                .withPseudoClass(MATCHING_GROUPS_NOT_SEARCH, entry -> entry.matchCategory().isEqualTo(MatchCategory.MATCHING_GROUPS_NOT_SEARCH))
                .withPseudoClass(NOT_MATCHING_SEARCH_AND_GROUPS, entry -> entry.matchCategory().isEqualTo(MatchCategory.NOT_MATCHING_SEARCH_AND_GROUPS))
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver)
                .setOnDragExited(this::handleOnDragExited)
                .setOnMouseDragEntered(this::handleOnDragEntered)
                .install(this);

        this.getSortOrder().clear();

        // force match category column to be the first sort order, (match_category column is always the first column)
        this.getSortOrder().addFirst(getColumns().getFirst());
        this.getSortOrder().addListener((ListChangeListener<TableColumn<BibEntryTableViewModel, ?>>) change -> {
                if (!this.getSortOrder().getFirst().equals(getColumns().getFirst())) {
                    this.getSortOrder().addFirst(getColumns().getFirst());
                }
            });

        mainTablePreferences.getColumnPreferences().getColumnSortOrder().forEach(columnModel ->
                this.getColumns().stream()
                    .map(column -> (MainTableColumn<?>) column)
                    .filter(column -> column.getModel().equals(columnModel))
                    .findFirst()
                    .ifPresent(column -> {
                        LOGGER.trace("Adding sort order for col {} ", column);
                        this.getSortOrder().add(column);
                    }));

        if (mainTablePreferences.getResizeColumnsToFit()) {
            this.setColumnResizePolicy(new SmartConstrainedResizePolicy());
        }

        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        this.setItems(model.getEntriesFilteredAndSorted());

        // Enable sorting
        model.getEntriesFilteredAndSorted().comparatorProperty().bind(this.comparatorProperty());

        this.getStylesheets().add(Objects.requireNonNull(MainTable.class.getResource("MainTable.css")).toExternalForm());

        // Store visual state
        new PersistenceVisualStateTable(this, mainTablePreferences.getColumnPreferences()).addListeners();

        setupKeyBindings(keyBindingRepository);

        this.setOnKeyTyped(key -> {
            if (this.getSortOrder().size() <= 1) {
                return;
            }
            // skip match category column
            this.jumpToSearchKey(getSortOrder().get(1), key);
        });

        database.getDatabase().registerListener(this);

        // Enable the header right-click menu.
        new MainTableHeaderContextMenu(this, mainTableColumnFactory, tabContainer, dialogService).show(true);
    }

    /**
     * This is called, if a user starts typing some characters into the keyboard with focus on main table. The {@link MainTable} will scroll to the cell with the same starting column value and typed string
     * If the user presses any other special key as well, e.g. alt or shift we don't jump
     *
     * @param sortedColumn The sorted column in {@link MainTable}
     * @param keyEvent     The pressed character
     */
    private void jumpToSearchKey(TableColumn<BibEntryTableViewModel, ?> sortedColumn, KeyEvent keyEvent) {
        if (keyEvent.isAltDown() || keyEvent.isControlDown() || keyEvent.isMetaDown() || keyEvent.isShiftDown()) {
            return;
        }
        if ((keyEvent.getCharacter() == null) || (sortedColumn == null)) {
            return;
        }

        if ((System.currentTimeMillis() - lastKeyPressTime) < 700) {
            columnSearchTerm += keyEvent.getCharacter().toLowerCase();
        } else {
            columnSearchTerm = keyEvent.getCharacter().toLowerCase();
        }

        lastKeyPressTime = System.currentTimeMillis();

        this.getItems().stream()
            .filter(item -> Optional.ofNullable(sortedColumn.getCellObservableValue(item).getValue())
                                    .map(Object::toString)
                                    .orElse("")
                                    .toLowerCase()
                                    .startsWith(columnSearchTerm))
            .findFirst()
            .ifPresent(item -> {
                getSelectionModel().clearSelection();
                getSelectionModel().select(item);
                scrollTo(item);
            });
    }

    @Subscribe
    public void listen(EntriesAddedEvent event) {
        UiTaskExecutor.runInJavaFXThread(() -> clearAndSelect(event.getFirstEntry()));
    }

    public void clearAndSelect(BibEntry bibEntry) {
        getSelectionModel().clearSelection();
        findEntry(bibEntry).ifPresent(entry -> {
            getSelectionModel().select(entry);
            scrollTo(entry);
        });
    }

    public void copy() {
        List<BibEntry> selectedEntries = getSelectedEntries();

        if (!selectedEntries.isEmpty()) {
            List<BibtexString> stringConstants = getUsedStringValues(selectedEntries);
            try {
                if (stringConstants.isEmpty()) {
                    clipBoardManager.setContent(selectedEntries, entryTypesManager);
                } else {
                    clipBoardManager.setContent(selectedEntries, entryTypesManager, stringConstants);
                }
                dialogService.notify(Localization.lang("Copied %0 entry(ies)", selectedEntries.size()));
            } catch (IOException e) {
                LOGGER.error("Error while copying selected entries to clipboard.", e);
            }
        }
    }

    public void cut() {
        copy();
        libraryTab.delete(StandardActions.CUT);
    }

    private void scrollToNextMatchCategory() {
        BibEntryTableViewModel selectedEntry = getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            return;
        }

        MatchCategory currentMatchCategory = selectedEntry.matchCategory().get();
        for (int i = getSelectionModel().getSelectedIndex(); i < getItems().size(); i++) {
            if (getItems().get(i).matchCategory().get() != currentMatchCategory) {
                getSelectionModel().clearSelection();
                getSelectionModel().select(i);
                scrollTo(i);
                return;
            }
        }
    }

    private void scrollToPreviousMatchCategory() {
        BibEntryTableViewModel selectedEntry = getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            return;
        }

        MatchCategory currentMatchCategory = selectedEntry.matchCategory().get();
        for (int i = getSelectionModel().getSelectedIndex(); i >= 0; i--) {
            if (getItems().get(i).matchCategory().get() != currentMatchCategory) {
                MatchCategory targetMatchCategory = getItems().get(i).matchCategory().get();
                // found the previous category, scroll to the first entry of that category
                while ((i >= 0) && getItems().get(i).matchCategory().get() == targetMatchCategory) {
                    i--;
                }
                getSelectionModel().clearSelection();
                getSelectionModel().select(i + 1);
                scrollTo(i + 1);
                return;
            }
        }
    }

    private void setupKeyBindings(KeyBindingRepository keyBindings) {
        EditAction pasteAction = new EditAction(StandardActions.PASTE, () -> libraryTab, stateManager, undoManager);
        EditAction copyAction = new EditAction(StandardActions.COPY, () -> libraryTab, stateManager, undoManager);
        EditAction cutAction = new EditAction(StandardActions.CUT, () -> libraryTab, stateManager, undoManager);
        EditAction deleteAction = new EditAction(StandardActions.DELETE_ENTRY, () -> libraryTab, stateManager, undoManager);

        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                getSelectedEntries().stream()
                                    .findFirst()
                                    .ifPresent(libraryTab::showAndEdit);
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
                        pasteAction.execute();
                        event.consume();
                        break;
                    case COPY:
                        copyAction.execute();
                        event.consume();
                        break;
                    case CUT:
                        cutAction.execute();
                        event.consume();
                        break;
                    case DELETE_ENTRY:
                        deleteAction.execute();
                        event.consume();
                        break;
                    case SCROLL_TO_NEXT_MATCH_CATEGORY:
                        scrollToNextMatchCategory();
                        event.consume();
                        break;
                    case SCROLL_TO_PREVIOUS_MATCH_CATEGORY:
                         scrollToPreviousMatchCategory();
                        event.consume();
                        break;
                    default:
                        // Pass other keys to parent
                }
            }
        });
    }

    public void clearAndSelectFirst() {
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
        List<BibEntry> entriesToAdd;
        String content = ClipBoardManager.getContents();
        entriesToAdd = importHandler.handleBibTeXData(content);
        if (entriesToAdd.isEmpty()) {
            entriesToAdd = handleNonBibTeXStringData(content);
        }
        if (entriesToAdd.isEmpty()) {
            return;
        }

        importHandler.importEntriesWithDuplicateCheck(database, entriesToAdd);
    }

    private List<BibEntry> handleNonBibTeXStringData(String data) {
        try {
            return this.importHandler.handleStringData(data);
        } catch (FetcherException exception) {
            if (exception instanceof FetcherClientException) {
                dialogService.showInformationDialogAndWait(Localization.lang("Look up identifier"), Localization.lang("No data was found for the identifier"));
            } else if (exception instanceof FetcherServerException) {
                dialogService.showInformationDialogAndWait(Localization.lang("Look up identifier"), Localization.lang("Server not available"));
            } else {
                dialogService.showErrorDialogAndWait(exception);
            }
            return List.of();
        }
    }

    public void dropEntry(List<BibEntry> entriesToAdd) {
        importHandler.importEntriesWithDuplicateCheck(database, entriesToAdd);
    }

    private void handleOnDragOver(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel item, DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
            ControlHelper.setDroppingPseudoClasses(row, event);
        }
        event.consume();
    }

    private void handleOnDragOverTableView(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    private void handleOnDragEntered(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel entry, MouseDragEvent event) {
        // Support the following gesture to select entries: click on one row -> hold mouse button -> move over other rows
        // We need to select all items between the starting row and the row where the user currently hovers the mouse over
        // It is not enough to just select the currently hovered row since then sometimes rows are not marked selected if the user moves to fast
        @SuppressWarnings("unchecked")
        TableRow<BibEntryTableViewModel> sourceRow = (TableRow<BibEntryTableViewModel>) event.getGestureSource();
        getSelectionModel().selectRange(sourceRow.getIndex(), row.getIndex());
    }

    private void handleOnDragExited(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel entry, DragEvent dragEvent) {
        ControlHelper.removeDroppingPseudoClasses(row);
    }

    private void handleOnDragDetected(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel entry, MouseEvent event) {
        // Start drag'n'drop
        row.startFullDrag();

        List<BibEntry> entries = getSelectionModel().getSelectedItems().stream().map(BibEntryTableViewModel::getEntry).collect(Collectors.toList());

        // The following is necessary to initiate the drag and drop in JavaFX,
        // although we don't need the contents, it does not work without
        // Drag'n'drop to other tabs use COPY TransferMode, drop to group sidepane use MOVE
        ClipboardContent content = new ClipboardContent();
        Dragboard dragboard = startDragAndDrop(TransferMode.COPY_OR_MOVE);
        content.put(DragAndDropDataFormats.ENTRIES, "");
        dragboard.setContent(content);

        if (!entries.isEmpty()) {
            localDragboard.putBibEntries(entries);
        }

        event.consume();
    }

    private void handleOnDragDropped(TableRow<BibEntryTableViewModel> row, BibEntryTableViewModel target, DragEvent event) {
        boolean success = false;

        if (event.getDragboard().hasFiles()) {
            List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());

            // Different actions depending on where the user releases the drop in the target row
            // Bottom + top -> import entries
            // Center -> link files to entry
            // Depending on the pressed modifier, move/copy/link files to drop target
            switch (ControlHelper.getDroppingMouseLocation(row, event)) {
                case TOP, BOTTOM -> importHandler.importFilesInBackground(files, database, filePreferences).executeWith(taskExecutor);
                case CENTER -> {
                    BibEntry entry = target.getEntry();
                    switch (event.getTransferMode()) {
                        case LINK -> {
                            LOGGER.debug("Mode LINK"); // shift on win or no modifier
                            importHandler.getLinker().addFilesToEntry(entry, files);
                        }
                        case MOVE -> {
                            LOGGER.debug("Mode MOVE"); // alt on win
                            importHandler.getLinker().moveFilesToFileDirRenameAndAddToEntry(entry, files, libraryTab.getLuceneManager());
                        }
                        case COPY -> {
                            LOGGER.debug("Mode Copy"); // ctrl on win
                            importHandler.getLinker().copyFilesToFileDirAndAddToEntry(entry, files, libraryTab.getLuceneManager());
                        }
                    }
                }
            }

            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void handleOnDragDroppedTableView(DragEvent event) {
        boolean success = false;

        if (event.getDragboard().hasFiles()) {
            List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).toList();
            importHandler.importFilesInBackground(files, this.database, filePreferences).executeWith(taskExecutor);
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    public void addSelectionListener(ListChangeListener<? super BibEntryTableViewModel> listener) {
        getSelectionModel().getSelectedItems().addListener(listener);
    }

    public MainTableDataModel getTableModel() {
        return model;
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

    private List<BibtexString> getUsedStringValues(List<BibEntry> entries) {
        return database.getDatabase().getUsedStrings(entries);
    }
}
