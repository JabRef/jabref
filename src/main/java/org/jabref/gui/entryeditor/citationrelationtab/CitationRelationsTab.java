package org.jabref.gui.entryeditor.citationrelationtab;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.entrychange.PreviewWithSourceTab;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.CitationFetcher;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.SemanticScholarFetcher;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.mergeentries.EntriesMergeResult;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.BibDatabaseModeDetection;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckListView;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI for tab displaying an articles citation relations in two lists based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    public static final String NAME = "Citation relations";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationRelationsTab.class);

    // Tasks used to implement asynchronous fetching of related articles
    private static BackgroundTask<List<BibEntry>> citingTask;
    private static BackgroundTask<List<BibEntry>> citedByTask;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final BibEntryRelationsRepository bibEntryRelationsRepository;
    private final CitationsRelationsTabViewModel citationsRelationsTabViewModel;
    private final DuplicateCheck duplicateCheck;
    private final BibEntryTypesManager entryTypesManager;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    public CitationRelationsTab(DialogService dialogService,
                                UndoManager undoManager,
                                StateManager stateManager,
                                FileUpdateMonitor fileUpdateMonitor,
                                GuiPreferences preferences,
                                TaskExecutor taskExecutor,
                                BibEntryTypesManager bibEntryTypesManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));
        setId("citationRelationsTab");

        this.entryTypesManager = bibEntryTypesManager;
        this.duplicateCheck = new DuplicateCheck(entryTypesManager);
        this.bibEntryRelationsRepository = new BibEntryRelationsRepository(new SemanticScholarFetcher(preferences.getImporterPreferences()),
                new BibEntryRelationsCache());
        citationsRelationsTabViewModel = new CitationsRelationsTabViewModel(preferences, undoManager, stateManager, dialogService, fileUpdateMonitor, taskExecutor);
    }

    /**
     * Method to create main SplitPane holding all lists, buttons and labels for tab and starts search
     *
     * @param entry BibEntry which is currently selected in JabRef Database
     * @return SplitPane to display
     */
    private SplitPane getPaneAndStartSearch(BibEntry entry) {
        // Create Layout Containers
        VBox citingVBox = new VBox();
        VBox citedByVBox = new VBox();
        citingVBox.setFillWidth(true);
        citedByVBox.setFillWidth(true);
        citingVBox.setAlignment(Pos.TOP_CENTER);
        citedByVBox.setAlignment(Pos.TOP_CENTER);
        AnchorPane citingHBox = new AnchorPane();
        citingHBox.setPrefHeight(40);
        AnchorPane citedByHBox = new AnchorPane();
        citedByHBox.setPrefHeight(40);

        // Create Heading Lab
        Label citingLabel = new Label(Localization.lang("Cites"));
        styleLabel(citingLabel);
        Label citedByLabel = new Label(Localization.lang("Cited By"));
        styleLabel(citedByLabel);

        // Create ListViews
        CheckListView<CitationRelationItem> citingListView = new CheckListView<>();
        CheckListView<CitationRelationItem> citedByListView = new CheckListView<>();

        // Create refresh Buttons for both sides
        Button refreshCitingButton = IconTheme.JabRefIcons.REFRESH.asButton();
        refreshCitingButton.setTooltip(new Tooltip(Localization.lang("Restart search")));
        styleTopBarNode(refreshCitingButton, 15.0);
        Button refreshCitedByButton = IconTheme.JabRefIcons.REFRESH.asButton();
        refreshCitedByButton.setTooltip(new Tooltip(Localization.lang("Restart search")));
        styleTopBarNode(refreshCitedByButton, 15.0);

        // Create abort buttons for both sides
        Button abortCitingButton = IconTheme.JabRefIcons.CLOSE.asButton();
        abortCitingButton.getGraphic().resize(30, 30);
        abortCitingButton.setTooltip(new Tooltip(Localization.lang("Cancel search")));
        styleTopBarNode(abortCitingButton, 15.0);
        Button abortCitedButton = IconTheme.JabRefIcons.CLOSE.asButton();
        abortCitedButton.getGraphic().resize(30, 30);
        abortCitedButton.setTooltip(new Tooltip(Localization.lang("Cancel search")));
        styleTopBarNode(abortCitedButton, 15.0);

        ProgressIndicator citingProgress = new ProgressIndicator();
        citingProgress.setMaxSize(25, 25);
        styleTopBarNode(citingProgress, 50.0);
        ProgressIndicator citedByProgress = new ProgressIndicator();
        citedByProgress.setMaxSize(25, 25);
        styleTopBarNode(citedByProgress, 50.0);

        // Create import buttons for both sides
        Button importCitingButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        importCitingButton.setTooltip(new Tooltip(Localization.lang("Add selected entries to database")));
        styleTopBarNode(importCitingButton, 50.0);
        Button importCitedByButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        importCitedByButton.setTooltip(new Tooltip(Localization.lang("Add selected entries to database")));
        styleTopBarNode(importCitedByButton, 50.0);
        hideNodes(importCitingButton, importCitedByButton);

        citingHBox.getChildren().addAll(citingLabel, refreshCitingButton, importCitingButton, citingProgress, abortCitingButton);
        citedByHBox.getChildren().addAll(citedByLabel, refreshCitedByButton, importCitedByButton, citedByProgress, abortCitedButton);

        VBox.setVgrow(citingListView, Priority.ALWAYS);
        VBox.setVgrow(citedByListView, Priority.ALWAYS);
        citingVBox.getChildren().addAll(citingHBox, citingListView);
        citedByVBox.getChildren().addAll(citedByHBox, citedByListView);

        refreshCitingButton.setOnMouseClicked(event -> searchForRelations(
            entry,
            citingListView,
            abortCitingButton,
            refreshCitingButton,
            CitationFetcher.SearchType.CITES,
            importCitingButton,
            citingProgress,
            true));

        refreshCitedByButton.setOnMouseClicked(event -> searchForRelations(entry, citedByListView, abortCitedButton,
                refreshCitedByButton, CitationFetcher.SearchType.CITED_BY, importCitedByButton, citedByProgress, true));

        // Create SplitPane to hold all nodes above
        SplitPane container = new SplitPane(citingVBox, citedByVBox);
        styleFetchedListView(citedByListView);
        styleFetchedListView(citingListView);

        searchForRelations(entry, citingListView, abortCitingButton, refreshCitingButton,
                CitationFetcher.SearchType.CITES, importCitingButton, citingProgress, false);

        searchForRelations(entry, citedByListView, abortCitedButton, refreshCitedByButton,
                CitationFetcher.SearchType.CITED_BY, importCitedByButton, citedByProgress, false);

        return container;
    }

    /**
     * Styles a given CheckListView to display BibEntries either with a hyperlink or an add button
     *
     * @param listView CheckListView to style
     */
    private void styleFetchedListView(CheckListView<CitationRelationItem> listView) {
        PseudoClass entrySelected = PseudoClass.getPseudoClass("selected");
        new ViewModelListCellFactory<CitationRelationItem>()
                .withGraphic(entry -> {

                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = BibEntryView.getEntryNode(entry.entry());
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox hContainer = new HBox();
                    hContainer.prefWidthProperty().bind(listView.widthProperty().subtract(25));

                    VBox vContainer = new VBox();

                    if (entry.isLocal()) {
                        hContainer.getStyleClass().add("duplicate-entry");
                        Button jumpTo = IconTheme.JabRefIcons.LINK.asButton();
                        jumpTo.setTooltip(new Tooltip(Localization.lang("Jump to entry in library")));
                        jumpTo.getStyleClass().add("addEntryButton");
                        jumpTo.setOnMouseClicked(event -> jumpToEntry(entry));
                        hContainer.setOnMouseClicked(event -> {
                                if (event.getClickCount() == 2) {
                                    jumpToEntry(entry);
                                }
                        });
                        vContainer.getChildren().add(jumpTo);

                        Button compareButton = IconTheme.JabRefIcons.MERGE_ENTRIES.asButton();
                        compareButton.setTooltip(new Tooltip(Localization.lang("Compare with existing entry")));
                        compareButton.setOnMouseClicked(event -> openPossibleDuplicateEntriesWindow(entry, listView));
                        vContainer.getChildren().add(compareButton);
                    } else {
                        ToggleButton addToggle = IconTheme.JabRefIcons.ADD.asToggleButton();
                        addToggle.setTooltip(new Tooltip(Localization.lang("Select entry")));
                        EasyBind.subscribe(addToggle.selectedProperty(), selected -> {
                            if (selected) {
                                addToggle.setGraphic(IconTheme.JabRefIcons.ADD_FILLED.withColor(IconTheme.SELECTED_COLOR).getGraphicNode());
                            } else {
                                addToggle.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
                            }
                        });
                        addToggle.getStyleClass().add("addEntryButton");
                        addToggle.selectedProperty().bindBidirectional(listView.getItemBooleanProperty(entry));
                        vContainer.getChildren().add(addToggle);
                    }

                    if (entry.entry().getDOI().isPresent() || entry.entry().getField(StandardField.URL).isPresent()) {
                        Button openWeb = IconTheme.JabRefIcons.OPEN_LINK.asButton();
                        openWeb.setTooltip(new Tooltip(Localization.lang("Open URL or DOI")));
                        openWeb.setOnMouseClicked(event -> {
                            String url = entry.entry().getDOI().flatMap(DOI::getExternalURI).map(URI::toString)
                                              .or(() -> entry.entry().getField(StandardField.URL)).orElse("");
                            if (StringUtil.isNullOrEmpty(url)) {
                                return;
                            }
                            try {
                                NativeDesktop.openBrowser(url, preferences.getExternalApplicationsPreferences());
                            } catch (IOException ex) {
                                dialogService.notify(Localization.lang("Unable to open link."));
                            }
                        });
                        vContainer.getChildren().addLast(openWeb);
                    }

                    Button showEntrySource = IconTheme.JabRefIcons.SOURCE.asButton();
                    showEntrySource.setTooltip(new Tooltip(Localization.lang("%0 source", "BibTeX")));
                    showEntrySource.setOnMouseClicked(event -> showEntrySourceDialog(entry.entry()));

                    vContainer.getChildren().addLast(showEntrySource);

                    hContainer.getChildren().addAll(entryNode, separator, vContainer);
                    hContainer.getStyleClass().add("entry-container");

                    return hContainer;
                })
                .withOnMouseClickedEvent((ee, event) -> {
                    if (!ee.isLocal()) {
                        listView.getCheckModel().toggleCheckState(ee);
                    }
                })
                .withPseudoClass(entrySelected, listView::getItemBooleanProperty)
                .install(listView);

        listView.setSelectionModel(new NoSelectionModel<>());
    }

    private void jumpToEntry(CitationRelationItem entry) {
        citingTask.cancel();
        citedByTask.cancel();
        stateManager.activeTabProperty().get().ifPresent(tab -> tab.showAndEdit(entry.localEntry()));
    }

    /**
     * @implNote This code is similar to {@link PreviewWithSourceTab#getSourceString(BibEntry, BibDatabaseMode, FieldPreferences, BibEntryTypesManager)}.
     */
    private String getSourceString(BibEntry entry, BibDatabaseMode type, FieldPreferences fieldPreferences, BibEntryTypesManager entryTypesManager) throws IOException {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldPreferences);
        new BibEntryWriter(fieldWriter, entryTypesManager).write(entry, bibWriter, type);
        return writer.toString();
    }

    private void showEntrySourceDialog(BibEntry entry) {
        CodeArea ca = new CodeArea();
        try {
            BibDatabaseMode mode = stateManager.getActiveDatabase().map(BibDatabaseContext::getMode)
                                               .orElse(BibDatabaseMode.BIBLATEX);
            ca.appendText(getSourceString(entry, mode, preferences.getFieldPreferences(), this.entryTypesManager));
        } catch (IOException e) {
            LOGGER.warn("Incorrect entry, could not load source:", e);
            return;
        }

        ca.setWrapText(true);
        ca.setPadding(new Insets(0, 10, 0, 10));
        ca.showParagraphAtTop(0);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(new VirtualizedScrollPane<>(ca));

        DialogPane dialogPane = new DialogPane();
        dialogPane.setPrefSize(800, 400);
        dialogPane.setContent(scrollPane);
        String title = Localization.lang("Show BibTeX source");

        dialogService.showCustomDialogAndWait(title, dialogPane, ButtonType.OK);
    }

    /**
     * Method to style heading labels
     *
     * @param label label to style
     */
    private void styleLabel(Label label) {
        label.setStyle("-fx-padding: 5px");
        label.setAlignment(Pos.CENTER);
        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setBottomAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);
    }

    /**
     * Method to style refresh buttons
     *
     * @param node node to style
     */
    private void styleTopBarNode(Node node, double offset) {
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, offset);
    }

    /**
     * Determines if tab should be shown according to preferences
     *
     * @param entry Currently selected BibEntry
     * @return whether tab should be shown
     */
    @Override
    public boolean shouldShow(BibEntry entry) {
        // TODO: Create a preference and show tab only if preference is enabled
        return true;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        setContent(getPaneAndStartSearch(entry));
    }

    /**
     * Method to start search for relations and display them in the associated ListView
     *
     * @param entry         BibEntry currently selected in Jabref Database
     * @param listView      ListView to use
     * @param abortButton   Button to stop the search
     * @param refreshButton refresh Button to use
     * @param searchType    type of search (CITING / CITEDBY)
     */
    private void searchForRelations(BibEntry entry, CheckListView<CitationRelationItem> listView, Button abortButton,
                                    Button refreshButton, CitationFetcher.SearchType searchType, Button importButton,
                                    ProgressIndicator progress, boolean shouldRefresh) {
        if (entry.getDOI().isEmpty()) {
            hideNodes(abortButton, progress);
            showNodes(refreshButton);
            listView.getItems().clear();
            listView.setPlaceholder(
                    new Label(Localization.lang("The selected entry doesn't have a DOI linked to it. Lookup a DOI and try again.")));
            return;
        }

        ObservableList<CitationRelationItem> observableList = FXCollections.observableArrayList();

        listView.setItems(observableList);

        if (citingTask != null && !citingTask.isCancelled() && searchType == CitationFetcher.SearchType.CITES) {
            citingTask.cancel();
        } else if (citedByTask != null && !citedByTask.isCancelled() && searchType == CitationFetcher.SearchType.CITED_BY) {
            citedByTask.cancel();
        }

        BackgroundTask<List<BibEntry>> task;

        if (searchType == CitationFetcher.SearchType.CITES) {
            task = BackgroundTask.wrap(() -> {
                if (shouldRefresh) {
                    bibEntryRelationsRepository.forceRefreshReferences(entry);
                }
                return bibEntryRelationsRepository.getReferences(entry);
            });
            citingTask = task;
        } else {
            task = BackgroundTask.wrap(() -> {
                if (shouldRefresh) {
                    bibEntryRelationsRepository.forceRefreshCitations(entry);
                }
                return bibEntryRelationsRepository.getCitations(entry);
            });
            citedByTask = task;
        }

        task.onRunning(() -> prepareToSearchForRelations(abortButton, refreshButton, importButton, progress, task))
            .onSuccess(fetchedList -> onSearchForRelationsSucceed(entry, listView, abortButton, refreshButton, searchType, importButton, progress, fetchedList, observableList))
            .onFailure(exception -> {
                LOGGER.error("Error while fetching citing Articles", exception);
                hideNodes(abortButton, progress, importButton);
                listView.setPlaceholder(new Label(Localization.lang("Error while fetching citing entries: %0",
                        exception.getMessage())));

                refreshButton.setVisible(true);
                dialogService.notify(exception.getMessage());
            })
            .executeWith(taskExecutor);
    }

    private void onSearchForRelationsSucceed(BibEntry entry, CheckListView<CitationRelationItem> listView,
                                             Button abortButton, Button refreshButton,
                                             CitationFetcher.SearchType searchType, Button importButton,
                                             ProgressIndicator progress, List<BibEntry> fetchedList,
                                             ObservableList<CitationRelationItem> observableList) {
        hideNodes(abortButton, progress);

        BibDatabase database = stateManager.getActiveDatabase().map(BibDatabaseContext::getDatabase)
                                           .orElse(new BibDatabase());
        observableList.setAll(
                fetchedList.stream().map(entr ->
                                   duplicateCheck.containsDuplicate(
                                                         database,
                                                         entr,
                                                         BibDatabaseModeDetection.inferMode(database))
                                                 .map(localEntry -> new CitationRelationItem(entr, localEntry, true))
                                                 .orElseGet(() -> new CitationRelationItem(entr, false)))
                           .toList()
        );

        if (!observableList.isEmpty()) {
            listView.refresh();
        } else {
            Label placeholder = new Label(Localization.lang("No articles found"));
            listView.setPlaceholder(placeholder);
        }
        BooleanBinding booleanBind = Bindings.isEmpty(listView.getCheckModel().getCheckedItems());
        importButton.disableProperty().bind(booleanBind);
        importButton.setOnMouseClicked(event -> importEntries(listView.getCheckModel().getCheckedItems(), searchType, entry));
        showNodes(refreshButton, importButton);
    }

    private void prepareToSearchForRelations(Button abortButton, Button refreshButton, Button importButton,
                                             ProgressIndicator progress, BackgroundTask<List<BibEntry>> task) {
        showNodes(abortButton, progress);
        hideNodes(refreshButton, importButton);

        abortButton.setOnAction(event -> {
            hideNodes(abortButton, progress, importButton);
            showNodes(refreshButton);
            task.cancel();
            dialogService.notify(Localization.lang("Search aborted!"));
        });
    }

    private void hideNodes(Node... nodes) {
        Arrays.stream(nodes).forEach(node -> node.setVisible(false));
    }

    private void showNodes(Node... nodes) {
        Arrays.stream(nodes).forEach(node -> node.setVisible(true));
    }

    /**
     * Function to import selected entries to the database. Also writes the entries to import to the CITING/CITED field
     *
     * @param entriesToImport entries to import
     */
    private void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry existingEntry) {
        citingTask.cancel();
        citedByTask.cancel();

        citationsRelationsTabViewModel.importEntries(entriesToImport, searchType, existingEntry);

        dialogService.notify(Localization.lang("%0 entry(s) imported", entriesToImport.size()));
    }

    /**
     * Function to open possible duplicate entries window to compare duplicate entries
     *
     * @param citationRelationItem duplicate in the citation relations tab
     * @param listView CheckListView to display citations
     */
    private void openPossibleDuplicateEntriesWindow(CitationRelationItem citationRelationItem, CheckListView<CitationRelationItem> listView) {
        BibEntry libraryEntry = citationRelationItem.localEntry();
        BibEntry citationEntry = citationRelationItem.entry();
        String leftHeader = Localization.lang("Library Entry");
        String rightHeader = Localization.lang("Citation Entry");

        MergeEntriesDialog dialog = new MergeEntriesDialog(libraryEntry, citationEntry, leftHeader, rightHeader, preferences);
        dialog.setTitle(Localization.lang("Possible duplicate entries"));

        Optional<EntriesMergeResult> entriesMergeResult = dialogService.showCustomDialogAndWait(dialog);
        entriesMergeResult.ifPresentOrElse(mergeResult -> {

            BibEntry mergedEntry = mergeResult.mergedEntry();
            // update local entry of selected citation relation item
            listView.getItems().set(listView.getItems().indexOf(citationRelationItem), new CitationRelationItem(citationRelationItem.entry(), mergedEntry, true));

            // Merge method is similar to MergeTwoEntriesAction#execute
            Optional<LibraryTab> libraryTab = stateManager.activeTabProperty().get();
            if (libraryTab.isEmpty()) {
                dialogService.notify(Localization.lang("No library present"));
                return;
            }

            BibDatabase database = libraryTab.get().getDatabase();
            database.removeEntry(mergeResult.originalLeftEntry());
            libraryTab.get().getMainTable().setCitationMergeMode(true);
            database.insertEntry(mergedEntry);

            NamedCompound ce = new NamedCompound(Localization.lang("Merge entries"));
            ce.addEdit(new UndoableRemoveEntries(database, mergeResult.originalLeftEntry()));
            ce.addEdit(new UndoableInsertEntries(database, mergedEntry));
            ce.end();

            undoManager.addEdit(ce);

            dialogService.notify(Localization.lang("Merged entries"));
        }, () -> dialogService.notify(Localization.lang("Canceled merging entries")));
    }
}
