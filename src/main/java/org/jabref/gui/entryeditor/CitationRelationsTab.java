package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.CitationFetcher;
import org.jabref.logic.importer.fetcher.OpenCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI for tab displaying an articles citation relations in two lists based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationRelationsTab.class);

    // Tasks used to implement asynchronous fetching of related articles
    private static BackgroundTask<List<BibEntry>> citingTask;
    private static BackgroundTask<List<BibEntry>> citedByTask;

    private final EntryEditorPreferences preferences;
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final PreferencesService preferencesService;
    private final LibraryTab libraryTab;

    /**
     * Class to hold a BibEntry and a boolean value whether it is already in the current database or not.
     */
    public static class CitationRelationItem {
        private final BibEntry entry;
        private final boolean isLocal;

        public CitationRelationItem(BibEntry entry, boolean isLocal) {
            this.entry = entry;
            this.isLocal = isLocal;
        }

        public BibEntry getEntry() {
            return entry;
        }

        public boolean isLocal() {
            return isLocal;
        }
    }

    public CitationRelationsTab(EntryEditorPreferences preferences, DialogService dialogService, BibDatabaseContext databaseContext, UndoManager undoManager, StateManager stateManager, FileUpdateMonitor fileUpdateMonitor, PreferencesService preferencesService, LibraryTab lTab) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.preferencesService = preferencesService;
        this.libraryTab = lTab;
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));
    }

    /**
     * The Pane that is shown when the functionality is not activated
     *
     * @param entry entry that is the context
     * @return StackPane that is the activation screen
     */
    private StackPane getActivationPane(BibEntry entry) {
        StackPane activation = new StackPane();
        activation.setId("citation-relation-tab");
        VBox alignment = new VBox();
        alignment.setId("activation-alignment");
        alignment.setFillWidth(true);
        alignment.setAlignment(Pos.BASELINE_CENTER);
        Label infoLabel = new Label(Localization.lang("The search is currently deactivated."));
        Button activate = new Button(Localization.lang("Activate"));
        activate.setOnAction(
                event -> {
                    JabRefPreferences prefs = JabRefPreferences.getInstance();
                    prefs.putBoolean(JabRefPreferences.ACTIVATE_CITATIONRELATIONS, true);
                    dialogService.notify(Localization.lang("Please restart JabRef for preferences to take effect."));
                    bindToEntry(entry);
                });
        activate.setDefaultButton(true);
        alignment.getChildren().addAll(infoLabel, activate);
        activation.getChildren().add(alignment);
        return activation;
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
        Label citingLabel = new Label(Localization.lang("Citing"));
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
        setVisibility(false, importCitingButton, importCitedByButton);

        citingHBox.getChildren().addAll(citingLabel, refreshCitingButton, importCitingButton, citingProgress, abortCitingButton);
        citedByHBox.getChildren().addAll(citedByLabel, refreshCitedByButton, importCitedByButton, citedByProgress, abortCitedButton);

        VBox.setVgrow(citingListView, Priority.ALWAYS);
        VBox.setVgrow(citedByListView, Priority.ALWAYS);
        citingVBox.getChildren().addAll(citingHBox, citingListView);
        citedByVBox.getChildren().addAll(citedByHBox, citedByListView);

        refreshCitingButton.setOnMouseClicked(event -> searchForRelations(entry, citingListView, abortCitingButton, refreshCitingButton, CitationFetcher.SearchType.CITING, importCitingButton, citingProgress));
        refreshCitedByButton.setOnMouseClicked(event -> searchForRelations(entry, citedByListView, abortCitedButton, refreshCitedByButton, CitationFetcher.SearchType.CITED_BY, importCitedByButton, citedByProgress));

        // Create SplitPane to hold all nodes above
        SplitPane container = new SplitPane(citedByVBox, citingVBox);

        styleFetchedListView(citingListView);
        styleFetchedListView(citedByListView);

        searchForRelations(entry, citingListView, abortCitingButton, refreshCitingButton, CitationFetcher.SearchType.CITING, importCitingButton, citingProgress);
        searchForRelations(entry, citedByListView, abortCitedButton, refreshCitedByButton, CitationFetcher.SearchType.CITED_BY, importCitedByButton, citedByProgress);

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
                    Node entryNode = BibEntryView.getEntryNode(entry.getEntry());
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox hContainer = new HBox();
                    hContainer.prefWidthProperty().bind(listView.widthProperty().subtract(25));

                    if (entry.isLocal()) {
                        Button jumpTo = IconTheme.JabRefIcons.LINK.asButton();
                        jumpTo.setTooltip(new Tooltip(Localization.lang("Jump to entry in database")));
                        jumpTo.getStyleClass().add("addEntryButton");
                        jumpTo.setOnMouseClicked(event -> {
                            libraryTab.showAndEdit(entry.getEntry());
                            libraryTab.clearAndSelect(entry.getEntry());
                            citingTask.cancel();
                            citedByTask.cancel();
                        });
                        hContainer.getChildren().addAll(entryNode, separator, jumpTo);
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
                        hContainer.getChildren().addAll(entryNode, separator, addToggle);
                    }
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
        return preferences.shouldShowCitationRelationsTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (preferences.isCitationRelationActivated()) {
            setContent(getPaneAndStartSearch(entry));
        } else {
            setContent(getActivationPane(entry));
        }
    }

    /**
     * Method to start search for relations and display them in the associated ListView
     *
     * @param entry         BibEntry currently selected in Jabref Database
     * @param listView      ListView to use
     * @param abort         Button to stop the search
     * @param refreshButton refresh Button to use
     * @param searchType    type of search (CITING / CITEDBY)
     */
    private void searchForRelations(BibEntry entry, CheckListView<CitationRelationItem> listView, Button abort, Button refreshButton, CitationFetcher.SearchType searchType, Button importButton, ProgressIndicator progress) {
        // Check if current entry has DOI Number required for searching
        if (entry.getField(StandardField.DOI).isPresent()) {

            ObservableList<CitationRelationItem> observableList = FXCollections.observableArrayList();

            listView.getItems().clear();

            // Perform search in background and deal with success or failure
            List<BibEntry> localList = runOfflineTask(entry, searchType.toStandardField());
            if (!localList.isEmpty()) {
                observableList.addAll(localList.stream().map(localEntry -> new CitationRelationItem(localEntry, true)).collect(Collectors.toList()));
            }
            listView.setItems(observableList);

            if (citingTask != null && !citingTask.isCanceled() && searchType.equals(CitationFetcher.SearchType.CITING)) {
                citingTask.cancel();
            } else if (citedByTask != null && !citedByTask.isCanceled() && searchType.equals(CitationFetcher.SearchType.CITED_BY)) {
                citedByTask.cancel();
            }

            OpenCitationFetcher fetcher = new OpenCitationFetcher();
            BackgroundTask<List<BibEntry>> task;

            if (searchType.equals(CitationFetcher.SearchType.CITING)) {
                task = BackgroundTask.wrap(() -> fetcher.searchCiting(entry));
                citingTask = task;
            } else {
                task = BackgroundTask.wrap(() -> fetcher.searchCitedBy(entry));
                citedByTask = task;
            }

            task.onRunning(() -> {
                    setVisibility(true, abort, progress);
                    setVisibility(false, refreshButton, importButton);
                    abort.setOnMouseClicked(event -> {
                        task.cancel();
                        setVisibility(false, abort, progress, importButton);
                        dialogService.notify(Localization.lang("Search aborted!"));
                        refreshButton.setVisible(true);
                    });
                })
                .onSuccess(fetchedList -> {
                    setVisibility(false, abort, progress);
                    if (!fetchedList.isEmpty()) {
                        filterDifference(fetchedList, observableList, searchType, entry);
                    }
                    if (!observableList.isEmpty()) {
                        listView.refresh();
                    } else {
                        Label placeholder = new Label(Localization.lang("No articles found"));
                        listView.setPlaceholder(placeholder);
                    }
                    BooleanBinding booleanBind = Bindings.isEmpty(listView.getCheckModel().getCheckedItems());
                    importButton.disableProperty().bind(booleanBind);
                    importButton.setOnMouseClicked(event -> importEntries(listView.getCheckModel().getCheckedItems(), searchType, entry));
                    setVisibility(true, refreshButton, importButton);
                })
                .onFailure(exception -> {
                    LOGGER.error("Error while fetching citing Articles", exception);
                    setVisibility(false, abort, progress, importButton);
                    refreshButton.setVisible(true);
                    dialogService.notify(exception.getMessage());
                })
                .executeWith(Globals.TASK_EXECUTOR);
        } else {
            dialogService.notify(Localization.lang("A DOI is required. Please add a DOI to the entry before searching."));
        }
    }

    /**
     * Sets visibility of given Nodes to the given value
     *
     * @param visibility visibility (true/false)
     * @param nodes      Nodes to apply new visibility
     */
    private void setVisibility(boolean visibility, Node... nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                node.setVisible(visibility);
            }
        }
    }

    /**
     * Performs a local lookup of fields in StandardField.CITING/CITED_BY
     *
     * @param entry Current Entry Context
     * @param field The StandardField to work with
     */
    List<BibEntry> runOfflineTask(BibEntry entry, StandardField field) {
        List<String> keys = getFilteredKeys(entry, field);
        LOGGER.atDebug()
              .addArgument(() -> field.getName())
              .addArgument(() -> keys.toString())
              .log("Current keys/DOI in {}: {}");
        return keys.stream()
                   .map(key -> getEntryByDOI(key))
                   .flatMap(Optional::stream)
                   .collect(Collectors.toList());
    }

    /**
     * Filters an observable list for entries that are already in the operator field of the entry.
     * If the entry is no duplicate, it also added the current entry to the negative operator of the entry in the list
     *
     * @param newEntries The List to Filter
     * @param operator   StandardField.CITING/CITED
     * @param entry      Current Entry Context
     */
    void filterDifference(List<BibEntry> newEntries, ObservableList<CitationRelationItem> observableList, CitationFetcher.SearchType operator, BibEntry entry) {
        StandardField field;
        StandardField nField;
        if (operator.equals(CitationFetcher.SearchType.CITED_BY)) {
            field = StandardField.CITED_BY;
            nField = StandardField.CITING;
        } else {
            field = StandardField.CITING;
            nField = StandardField.CITED_BY;
        }
        List<String> currentKeys = getFilteredKeys(entry, field); // Current existent entry.DOIs in Field
        for (BibEntry b : newEntries) {
            Optional<String> key = b.getField(StandardField.DOI);
            Optional<String> entryKey = entry.getField(StandardField.DOI);
            if (key.isPresent() && entryKey.isPresent()) { // Just proceed if doi is present
                String doi = key.get();
                String entryDoi = entryKey.get();
                if (!currentKeys.contains(doi) && getEntryByDOI(doi).isEmpty()) { // if its not in the already referenced keys and not in the database = new Article
                    b.setField(nField, getFilteredKeys(b, nField) + "," + entryDoi);
                    observableList.add(new CitationRelationItem(b, false));
                } else {
                    if (!currentKeys.contains(doi)) { // if in database but not in keys
                        entry.setField(field, entry.getField(field).orElse("") + "," + doi);
                        // Add negative Reference to existing Entry and add this entry as local reference
                        Optional<BibEntry> existing = getEntryByDOI(doi);
                        existing.ifPresent(bibEntry -> {
                            bibEntry.setField(nField, bibEntry.getField(nField).orElse("") + "," + entryDoi);
                            observableList.add(0, new CitationRelationItem(bibEntry, true));
                        });
                    }
                }
            }
        }
    }

    /**
     * Reads the CITING or CITED_BY field, extracts the CitationKeys, checks whether the according entry still exists
     * in database, set the new existing keys and return them
     *
     * @param entry    The currently selected entry
     * @param operator StandardField.CITING/CITED_BY
     * @return A List Containing the keys in the "operator"  field, theirs relations are in the Database
     */
    List<String> getFilteredKeys(BibEntry entry, StandardField operator) {
        Optional<String> citingS = entry.getField(operator);
        if (citingS.isEmpty()) {
            LOGGER.atDebug()
                  .addArgument(() -> entry.getCitationKey().orElse("no key"))
                  .addArgument(() -> operator.getName())
                  .log("{}: {} is empty!");
            return Collections.emptyList();
        }

        Arrays.asList(citingS.get().split(",")).stream()
              .filter(key -> getEntryByDOI(doi).isEmpty());

        ArrayList<String> keys = new ArrayList<>();
        // filterNonExisting(keys);
        entry.setField(operator, String.join(",", keys));
        LOGGER.debug("{}: {}: {}", entry.getField(StandardField.TITLE).orElse("no title"), operator.getName(), String.join(",", keys));
        return keys;
    }

    /**
     * Returns the Entry in the database with the given DOI, or null if no such entry exists
     *
     * @param doi doi TO LOOK for
     * @return null or found Entry
     */
    Optional<BibEntry> getEntryByDOI(String doi) {
        return databaseContext.getEntries().stream().
                              filter(entry -> doi.equals(entry.getField(StandardField.DOI).orElse(""))).findFirst();
    }

    /**
     * Returns a String containing the DOIs of a list of entries. Ignores entries with no DOI.
     *
     * @param entries The list of BibEntries to serialize
     * @return A comma separated list of CitationKeys (of the given list of entries)
     */
    static String serialize(List<BibEntry> entries) {
        return entries.stream()
                      .map(bibEntry -> bibEntry.getField(StandardField.DOI))
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(Collectors.joining(","));
    }

    /**
     * Function to import selected entries to the database. Also writes the entries to import to the CITING/CITED field
     *
     * @param entriesToImport entries to import
     */
    private void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry entry) {
        citingTask.cancel();
        citedByTask.cancel();
        List<BibEntry> entries = entriesToImport.stream().map(CitationRelationItem::getEntry).collect(Collectors.toList());
        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                ExternalFileTypes.getInstance(),
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager);
        importHandler.importEntries(entries);
        if (searchType.equals(CitationFetcher.SearchType.CITED_BY)) {
            entry.setField(StandardField.CITED_BY, serialize(entries));
        } else {
            entry.setField(StandardField.CITING, serialize(entries));
        }
        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }
}

