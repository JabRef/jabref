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
import javafx.scene.control.Hyperlink;
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
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
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
    private final SearchCitationsRelationsService searchCitationsRelationsService;
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
                                BibEntryTypesManager bibEntryTypesManager,
                                SearchCitationsRelationsService searchCitationsRelationsService) {
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
        this.searchCitationsRelationsService = searchCitationsRelationsService;

        this.citationsRelationsTabViewModel = new CitationsRelationsTabViewModel(
            preferences,
            undoManager,
            stateManager,
            dialogService,
            fileUpdateMonitor,
            taskExecutor
        );
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
        importCitingButton.setTooltip(new Tooltip(Localization.lang("Add selected entry(s) to library")));
        styleTopBarNode(importCitingButton, 50.0);
        Button importCitedByButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        importCitedByButton.setTooltip(new Tooltip(Localization.lang("Add selected entry(s) to library")));
        styleTopBarNode(importCitedByButton, 50.0);
        hideNodes(importCitingButton, importCitedByButton);

        citingHBox.getChildren().addAll(citingLabel, refreshCitingButton, importCitingButton, citingProgress, abortCitingButton);
        citedByHBox.getChildren().addAll(citedByLabel, refreshCitedByButton, importCitedByButton, citedByProgress, abortCitedButton);

        VBox.setVgrow(citingListView, Priority.ALWAYS);
        VBox.setVgrow(citedByListView, Priority.ALWAYS);
        citingVBox.getChildren().addAll(citingHBox, citingListView);
        citedByVBox.getChildren().addAll(citedByHBox, citedByListView);

        RelationSearchComponents citingRelationSearchComponents = new RelationSearchComponents(
                entry,
                citingListView,
                citedByListView,
                abortCitingButton,
                refreshCitingButton,
                CitationFetcher.SearchType.CITES,
                importCitingButton,
                citingProgress);

        RelationSearchComponents citedByRelationSearchComponents = new RelationSearchComponents(
                entry,
                citedByListView,
                citingListView,
                abortCitedButton,
                refreshCitedByButton,
                CitationFetcher.SearchType.CITED_BY,
                importCitedByButton,
                citedByProgress);

        refreshCitingButton.setOnMouseClicked(_ -> searchForRelations(citingRelationSearchComponents));
        refreshCitedByButton.setOnMouseClicked(_ -> searchForRelations(citedByRelationSearchComponents));

        // Create SplitPane to hold all nodes above
        SplitPane container = new SplitPane(citingVBox, citedByVBox);
        styleFetchedListView(citedByListView);
        styleFetchedListView(citingListView);

        searchForRelations(citingRelationSearchComponents);
        searchForRelations(citedByRelationSearchComponents);

        return container;
    }

    /**
     * Method to start search for relations and display them in the associated ListView
     *
     * @param relationSearchComponents components used to properly display panel
     */
    private void searchForRelations(RelationSearchComponents relationSearchComponents) {
        if (relationSearchComponents.entry().getDOI().isEmpty()) {
            setUpEmptyPanel(relationSearchComponents);
            return;
        }
        executeSearch(relationSearchComponents);
    }

    private void setUpEmptyPanel(RelationSearchComponents relationSearchComponents) {
        hideNodes(relationSearchComponents.abortButton(), relationSearchComponents.progress());
        showNodes(relationSearchComponents.refreshButton());

        HBox hBox = new HBox();
        Label label = new Label(Localization.lang("The selected entry doesn't have a DOI linked to it."));
        Hyperlink link = new Hyperlink(Localization.lang("Look Up a DOI and try again."));

        link.setOnAction(e -> {
            CrossRef doiFetcher = new CrossRef();

            BackgroundTask.wrap(() -> doiFetcher.findIdentifier(relationSearchComponents.entry()))
                          .onRunning(() -> {
                              setLabelOn(relationSearchComponents.listView(), "Looking Up DOI...");
                              setLabelOn(relationSearchComponents.otherListView(), "Looking Up DOI...");
                          })
                          .onSuccess(identifier -> {
                              if (identifier.isPresent()) {
                                  relationSearchComponents.entry().setField(StandardField.DOI, identifier.get().asString());
//                                  setLabelOn(relationSearchComponents.listView(), "Found DOI");
//                                  setLabelOn(relationSearchComponents.otherListView(), "Found DOI");
                                  executeSearch(relationSearchComponents);
                                  // fetch data separately
                              } else {
                                  // not found
                                  // just invoke setUpEmptyPanel on both relationSearch
                                  setLabelOn(relationSearchComponents.listView(), "No DOI Found");
                                  setLabelOn(relationSearchComponents.otherListView(), "No DOI Found");
                              }
                          }).onFailure(ex -> {
                              setLabelOn(relationSearchComponents.listView(), "Error " + ex.getMessage());
                              setLabelOn(relationSearchComponents.otherListView(), "Error " + ex.getMessage());
                          }).executeWith(taskExecutor);
        });

        hBox.getChildren().add(label);
        hBox.getChildren().add(link);
        hBox.setSpacing(2d);
        hBox.setStyle("-fx-alignment: center;");
        hBox.setFillHeight(true);

        relationSearchComponents.listView().getItems().clear();
        relationSearchComponents.listView().setPlaceholder(hBox);
    }

    private void executeSearch(RelationSearchComponents relationSearchComponents) {
        ObservableList<CitationRelationItem> observableList = FXCollections.observableArrayList();
        relationSearchComponents.listView().setItems(observableList);

        // TODO: It should not be possible to cancel a search task that is already running for same tab
        if (citingTask != null && !citingTask.isCancelled() && relationSearchComponents.searchType() == CitationFetcher.SearchType.CITES) {
            citingTask.cancel();
        } else if (citedByTask != null && !citedByTask.isCancelled() && relationSearchComponents.searchType() == CitationFetcher.SearchType.CITED_BY) {
            citedByTask.cancel();
        }

        this.createBackgroundTask(relationSearchComponents.entry(), relationSearchComponents.searchType())
            .consumeOnRunning(task -> prepareToSearchForRelations(relationSearchComponents, task))
            .onSuccess(fetchedList -> onSearchForRelationsSucceed(relationSearchComponents,
                    fetchedList,
                    observableList
            ))
            .onFailure(exception -> {
                LOGGER.error("Error while fetching citing Articles", exception);
                hideNodes(relationSearchComponents.abortButton(), relationSearchComponents.progress(), relationSearchComponents.importButton());
                relationSearchComponents.listView().setPlaceholder(new Label(Localization.lang("Error while fetching citing entries: %0",
                        exception.getMessage())));
                relationSearchComponents.refreshButton().setVisible(true);
                dialogService.notify(exception.getMessage());
            })
            .executeWith(taskExecutor);
    }

    private void prepareToSearchForRelations(RelationSearchComponents relationSearchComponents, BackgroundTask<List<BibEntry>> task) {
        showNodes(relationSearchComponents.abortButton(), relationSearchComponents.progress());
        hideNodes(relationSearchComponents.refreshButton(), relationSearchComponents.importButton());

        relationSearchComponents.abortButton().setOnAction(event -> {
            hideNodes(relationSearchComponents.abortButton(), relationSearchComponents.progress(), relationSearchComponents.importButton());
            showNodes(relationSearchComponents.refreshButton());
            task.cancel();
            dialogService.notify(Localization.lang("Search aborted!"));
        });
    }

    private void onSearchForRelationsSucceed(RelationSearchComponents relationSearchComponents,
                                             List<BibEntry> fetchedList,
                                             ObservableList<CitationRelationItem> observableList) {

        hideNodes(relationSearchComponents.abortButton(), relationSearchComponents.progress());

        BibDatabase database = stateManager.getActiveDatabase().map(BibDatabaseContext::getDatabase).orElse(new BibDatabase());
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
            relationSearchComponents.listView().refresh();
        } else {
            Label placeholder = new Label(Localization.lang("No articles found"));
            relationSearchComponents.listView().setPlaceholder(placeholder);
        }
        BooleanBinding booleanBind = Bindings.isEmpty(relationSearchComponents.listView().getCheckModel().getCheckedItems());
        relationSearchComponents.importButton().disableProperty().bind(booleanBind);
        relationSearchComponents.importButton().setOnMouseClicked(event -> importEntries(relationSearchComponents.listView().getCheckModel().getCheckedItems(), relationSearchComponents.searchType(), relationSearchComponents.entry()));
        showNodes(relationSearchComponents.refreshButton(), relationSearchComponents.importButton());
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
                        jumpTo.setOnMouseClicked(_ -> jumpToEntry(entry));
                        hContainer.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2) {
                                jumpToEntry(entry);
                            }
                        });
                        vContainer.getChildren().add(jumpTo);

                        Button compareButton = IconTheme.JabRefIcons.MERGE_ENTRIES.asButton();
                        compareButton.setTooltip(new Tooltip(Localization.lang("Compare with existing entry")));
                        compareButton.setOnMouseClicked(_ -> openPossibleDuplicateEntriesWindow(entry, listView));
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
                        openWeb.setOnMouseClicked(_ -> {
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
                    showEntrySource.setOnMouseClicked(_ -> showEntrySourceDialog(entry.entry()));

                    vContainer.getChildren().addLast(showEntrySource);

                    hContainer.getChildren().addAll(entryNode, separator, vContainer);
                    hContainer.getStyleClass().add("entry-container");

                    return hContainer;
                })
                .withOnMouseClickedEvent((citationRelationItem, _) -> {
                    if (!citationRelationItem.isLocal()) {
                        listView.getCheckModel().toggleCheckState(citationRelationItem);
                    }
                })
                .withPseudoClass(entrySelected, listView::getItemBooleanProperty)
                .install(listView);

        listView.setSelectionModel(new NoSelectionModel<>());
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

    private static void setLabelOn(CheckListView<CitationRelationItem> listView, String message) {
        Label lookingUpDoiLabel = new Label(message);
        listView.getItems().clear();
        listView.setPlaceholder(lookingUpDoiLabel);
    }

    /**
     * TODO: Make the method return a callable and let the calling method create the background task.
     */
    private BackgroundTask<List<BibEntry>> createBackgroundTask(
        BibEntry entry, CitationFetcher.SearchType searchType
    ) {
        return switch (searchType) {
            case CitationFetcher.SearchType.CITES -> {
                citingTask = BackgroundTask.wrap(
                    () -> this.searchCitationsRelationsService.searchReferences(entry)
                );
                yield citingTask;
            }
            case CitationFetcher.SearchType.CITED_BY -> {
                citedByTask = BackgroundTask.wrap(
                    () -> this.searchCitationsRelationsService.searchCitations(entry)
                );
                yield citedByTask;
            }
        };
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
