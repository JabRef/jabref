package org.jabref.gui.maintable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.externalfiles.FindUnlinkedFilesAction;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.importer.fetcher.LookupIdentifierAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.libraryproperties.LibraryPropertiesAction;
import org.jabref.gui.maintable.columns.LibraryColumn;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.ClipboardContentGenerator;
import org.jabref.gui.search.MatchCategory;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DragDrop;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
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
    private final StateManager stateManager;
    private final BibDatabaseContext database;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final MainTableDataModel model;
    private final CustomLocalDragboard localDragboard;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;
    private final FilePreferences filePreferences;
    private final ImportHandler importHandler;
    private final ClipboardContentGenerator clipboardContentGenerator;

    private long lastKeyPressTime;
    private String columnSearchTerm;
    private boolean citationMergeMode = false;

    /// There is one maintable instance per library tab
    public MainTable(MainTableDataModel model,
                     LibraryTab libraryTab,
                     LibraryTabContainer tabContainer,
                     @NonNull BibDatabaseContext database,
                     GuiPreferences preferences,
                     DialogService dialogService,
                     StateManager stateManager,
                     KeyBindingRepository keyBindingRepository,
                     ClipBoardManager clipBoardManager,
                     BibEntryTypesManager entryTypesManager,
                     TaskExecutor taskExecutor,
                     ImportHandler importHandler) {
        super();
        this.libraryTab = libraryTab;
        this.stateManager = stateManager;
        this.database = database;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.model = model;
        this.taskExecutor = taskExecutor;
        this.undoManager = libraryTab.getUndoManager();
        this.filePreferences = preferences.getFilePreferences();
        this.importHandler = importHandler;
        this.clipboardContentGenerator = new ClipboardContentGenerator(preferences.getPreviewPreferences(), preferences.getLayoutFormatterPreferences(), Injector.instantiateModelOrService(JournalAbbreviationRepository.class));

        MainTablePreferences mainTablePreferences = preferences.getMainTablePreferences();

        localDragboard = stateManager.getLocalDragboard();

        this.setOnDragOver(this::handleOnDragOverTableView);
        this.setOnDragDropped(this::handleOnDragDroppedTableView);

        this.getStyleClass().add("main-table");

        MainTableColumnFactory mainTableColumnFactory = new MainTableColumnFactory(
                database,
                preferences,
                preferences.getMainTableColumnPreferences(),
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
                        preferences,
                        undoManager,
                        clipBoardManager,
                        taskExecutor,
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                        entryTypesManager,
                        importHandler))
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

        Button addExampleButton = new Button(Localization.lang("Add example entry"));
        addExampleButton.getStyleClass().add("text-button-blue");
        addExampleButton.setOnAction(_ -> {
            BibEntry entry = addExampleEntry();
            libraryTab.showAndEdit(entry);
        });

        Button importPdfsButton = new Button(Localization.lang("Import existing PDFs"));
        importPdfsButton.getStyleClass().add("text-button-blue");
        importPdfsButton.setOnAction(_ -> importPdfs());

        Label noContentLabel = new Label(Localization.lang("No content in table"));
        noContentLabel.getStyleClass().add("welcome-header-label");

        HBox buttonBox = new HBox(20, addExampleButton, importPdfsButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox placeholderBox = new VBox(15, noContentLabel, buttonBox);
        placeholderBox.setAlignment(Pos.CENTER);

        updatePlaceholder(placeholderBox);

        database.getDatabase().getEntries().addListener((ListChangeListener<BibEntry>) change -> updatePlaceholder(placeholderBox));

        this.getItems().addListener((ListChangeListener<BibEntryTableViewModel>) change -> updatePlaceholder(placeholderBox));

        // Enable sorting
        // Workaround for a JavaFX bug: https://bugs.openjdk.org/browse/JDK-8301761 (The sorting of the SortedList can become invalid)
        // The default comparator of the SortedList does not consider the insertion index of entries that are equal according to the comparator.
        // When two entries are equal based on the comparator, the entry that was inserted first should be considered smaller.
        this.setSortPolicy(_ -> true);
        model.getEntriesFilteredAndSorted().comparatorProperty().bind(
                this.comparatorProperty().map(comparator -> {
                    if (comparator == null) {
                        return null;
                    }

                    return (entry1, entry2) -> {
                        int result = comparator.compare(entry1, entry2);
                        if (result != 0) {
                            return result;
                        }
                        // If the entries are equal according to the comparator, compare them by their index in the database.
                        // The comparison should ideally be based on the database index, but retrieving the index takes log(n). See {@link BibDatabase#indexOf}.
                        // Using the entry ID is also valid since IDs are monotonically increasing.
                        return entry1.getEntry().getId().compareTo(entry2.getEntry().getId());
                    };
                })
        );

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

    public void clearAndSelect(BibEntry bibEntry) {
        // check if entries merged from citation relations tab
        if (citationMergeMode) {
            // keep original entry selected and reset citation merge mode
            this.citationMergeMode = false;
        } else {
            // select new entry
            // Use Platform.runLater to avoid JavaFX bug with selection change events
            Platform.runLater(() -> {
                getSelectionModel().clearSelection();
                findEntry(bibEntry).ifPresent(entry -> {
                    getSelectionModel().select(entry);
                    scrollTo(entry);
                });
            });
        }
    }

    public void clearAndSelect(List<BibEntry> bibEntries) {
        // check if entries merged from citation relations tab
        if (citationMergeMode) {
            // keep original entry selected and reset citation merge mode
            this.citationMergeMode = false;
        } else {
            // Use Platform.runLater to avoid JavaFX bug with selection change events
            Platform.runLater(() -> {
                getSelectionModel().clearSelection();
                List<BibEntryTableViewModel> entries = bibEntries.stream()
                                                                 .filter(bibEntry -> bibEntry.getCitationKey().isPresent())
                                                                 .map(bibEntry -> findEntryByCitationKey(bibEntry.getCitationKey().get()))
                                                                 .filter(Optional::isPresent)
                                                                 .map(Optional::get)
                                                                 .toList();
                entries.forEach(entry -> getSelectionModel().select(entry));
                if (!entries.isEmpty()) {
                    scrollTo(entries.getFirst());
                }
            });
        }
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
        OpenUrlAction openUrlAction = new OpenUrlAction(dialogService, stateManager, preferences);
        OpenSelectedEntriesFilesAction openSelectedEntriesFilesActionFileAction = new OpenSelectedEntriesFilesAction(dialogService, stateManager, preferences, taskExecutor);
        MergeWithFetchedEntryAction mergeWithFetchedEntryAction = new MergeWithFetchedEntryAction(dialogService, stateManager, taskExecutor, preferences, undoManager);
        LookupIdentifierAction<DOI> lookupIdentifierAction = new LookupIdentifierAction<>(WebFetchers.getIdFetcherForIdentifier(DOI.class), stateManager, undoManager, dialogService, taskExecutor);

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
                    case OPEN_URL_OR_DOI:
                        openUrlAction.execute();
                        event.consume();
                        break;
                    case OPEN_FILE:
                        openSelectedEntriesFilesActionFileAction.execute();
                        event.consume();
                        break;
                    case MERGE_WITH_FETCHED_ENTRY:
                        mergeWithFetchedEntryAction.execute();
                        event.consume();
                        break;
                    case LOOKUP_DOC_IDENTIFIER:
                        lookupIdentifierAction.execute();
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

        ClipboardContent content;
        try {
            content = clipboardContentGenerator.generate(entries, CitationStyleOutputFormat.HTML, database);
        } catch (IOException e) {
            LOGGER.warn("Could not generate clipboard content. Falling back to empty clipboard", e);
            content = new ClipboardContent();
        }
        // Required to be able to drop the entries inside JabRef
        content.put(DragAndDropDataFormats.ENTRIES, "");

        // Drag'n'drop to other tabs use COPY TransferMode, drop to group sidepane use MOVE
        Dragboard dragboard = startDragAndDrop(TransferMode.COPY_OR_MOVE);
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

            // Depending on the pressed modifier, move/copy/link files to drop target
            // Modifiers do not work on macOS: https://bugs.openjdk.org/browse/JDK-8264172
            TransferMode transferMode = event.getTransferMode();

            switch (ControlHelper.getDroppingMouseLocation(row, event)) {
                // Different actions depending on where the user releases the drop in the target row
                // - Bottom + top -> import entries
                case TOP,
                     BOTTOM ->
                        importHandler.importFilesInBackground(files, database, filePreferences, transferMode).executeWith(taskExecutor);
                // - Center -> modify entry: link files to entry
                case CENTER -> {
                    BibEntry entry = target.getEntry();
                    ExternalFilesEntryLinker fileLinker = importHandler.getFileLinker();
                    DragDrop.handleDropOfFiles(files, transferMode, fileLinker, entry);
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
            importHandler
                    .importFilesInBackground(files, this.database, filePreferences, event.getTransferMode())
                    .executeWith(taskExecutor);
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
        return model.getViewModelByIndex(database.getDatabase().indexOf(entry));
    }

    private Optional<BibEntryTableViewModel> findEntryByCitationKey(String citationKey) {
        return model.getViewModelByCitationKey(citationKey);
    }

    public void setCitationMergeMode(boolean citationMerge) {
        this.citationMergeMode = citationMerge;
    }

    private void updatePlaceholder(VBox placeholderBox) {
        if (database.getDatabase().getEntries().isEmpty()) {
            this.setPlaceholder(placeholderBox);
            // [impl->req~maintable.focus~1]
            requestFocus();
        } else {
            this.setPlaceholder(null);
        }
    }

    private BibEntry addExampleEntry() {
        BibEntry exampleEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Oliver Kopp and Carl Christian Snethlage and Christoph Schwentker")
                .withField(StandardField.TITLE, "JabRef: BibTeX-based literature management software")
                .withField(StandardField.JOURNAL, "TUGboat")
                .withField(StandardField.VOLUME, "44")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "441--447")
                .withField(StandardField.DOI, "10.47397/tb/44-3/tb138kopp-jabref")
                .withField(StandardField.ISSN, "0896-3207")
                .withField(StandardField.ISSUE, "138")
                .withField(StandardField.YEAR, "2023")
                .withChanged(true);

        database.getDatabase().insertEntry(exampleEntry);
        return exampleEntry;
    }

    private void importPdfs() {
        List<Path> fileDirectories = database.getFileDirectories(filePreferences);

        if (fileDirectories.isEmpty()) {
            dialogService.notify(
                    Localization.lang("File directory is not set or does not exist.")
            );
            LibraryPropertiesAction libraryPropertiesAction = new LibraryPropertiesAction(stateManager);
            libraryPropertiesAction.execute();
            return;
        }

        FindUnlinkedFilesAction findUnlinkedFilesAction = new FindUnlinkedFilesAction(dialogService, stateManager);
        findUnlinkedFilesAction.execute();
    }
}
