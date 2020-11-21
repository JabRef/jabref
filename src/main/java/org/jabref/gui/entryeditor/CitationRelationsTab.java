package org.jabref.gui.entryeditor;

import java.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.TextFlowLimited;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.fetcher.CitationRelationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.JabRefPreferences;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckListView;

import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.undo.UndoManager;

/**
 * GUI for tab displaying an articles citation relations in two lists based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(org.jabref.gui.entryeditor.CitationRelationsTab.class);
    private final EntryEditorPreferences preferences;
    private final DialogService dialogService;
    private CheckListView<CitationRelationItem> citingListView;
    private CheckListView<CitationRelationItem> citedByListView;
    private Button refreshCitingButton;
    private Button refreshCitedByButton;
    private Button importCitingButton;
    private Button importCitedByButton;
    private Button abortCitingButton;
    private Button abortCitedButton;
    private ProgressIndicator citingProgress;
    private ProgressIndicator citedByProgress;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final PreferencesService preferencesService;
    private final LibraryTab libraryTab;

    //Tasks
    private static BackgroundTask<List<BibEntry>> citingTask;
    private static BackgroundTask<List<BibEntry>> citedByTask;

    public static class CitationRelationItem {
        private final BibEntry bibEntry;
        private final boolean local;

        public CitationRelationItem(BibEntry bibEntry, boolean local) {
            this.bibEntry = bibEntry;
            this.local = local;
        }

        public BibEntry getBibEntry() {
            return bibEntry;
        }

        public boolean isLocal() {
            return local;
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
        Label infoLabel = new Label(Localization.lang("The search is currently deactivated"));
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
     * Method to create main SplitPane holding all lists, buttons and labels for tab
     *
     * @param entry BibEntry which is currently selected in JabRef Database
     * @return SplitPane to display
     */
    private SplitPane getPane(BibEntry entry) {

        //Create Layout Containers
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

        //Create Heading Labels
        Label citingLabel = new Label("Citing");
        styleLabel(citingLabel);
        Label citedByLabel = new Label("Cited By");
        styleLabel(citedByLabel);

        //Create ListViews
        citingListView = new CheckListView<>();
        citedByListView = new CheckListView<>();

        //Create refresh Buttons for both sides
        refreshCitingButton = IconTheme.JabRefIcons.REFRESH.asButton();
        styleTopBarNode(refreshCitingButton, 15.0);
        refreshCitedByButton = IconTheme.JabRefIcons.REFRESH.asButton();
        styleTopBarNode(refreshCitedByButton, 15.0);
        //Create abort Buttons for both sides
        abortCitingButton = IconTheme.JabRefIcons.CLOSE.asButton();
        styleTopBarNode(abortCitingButton, 15.0);
        abortCitedButton = IconTheme.JabRefIcons.CLOSE.asButton();
        styleTopBarNode(abortCitedButton, 15.0);

        citingProgress = new ProgressIndicator();
        citingProgress.setMaxSize(25, 25);
        styleTopBarNode(citingProgress, 50.0);
        citedByProgress = new ProgressIndicator();
        citedByProgress.setMaxSize(25, 25);
        styleTopBarNode(citedByProgress, 50.0);


        //Create Import Buttons for both sides
        importCitingButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        importCitingButton.setVisible(false);
        styleTopBarNode(importCitingButton, 50.0);
        importCitedByButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        importCitedByButton.setVisible(false);
        styleTopBarNode(importCitedByButton, 50.0);

        citingHBox.getChildren().addAll(citingLabel, refreshCitingButton, importCitingButton, citingProgress, abortCitingButton);
        citedByHBox.getChildren().addAll(citedByLabel, refreshCitedByButton, importCitedByButton, citedByProgress, abortCitedButton);

        citingVBox.getChildren().addAll(citingHBox, citingListView);
        citedByVBox.getChildren().addAll(citedByHBox, citedByListView);

        refreshCitingButton.setOnMouseClicked(event -> searchForRelations(entry, citingListView, abortCitingButton, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, importCitingButton, citingProgress));
        refreshCitedByButton.setOnMouseClicked(event -> searchForRelations(entry, citedByListView, abortCitedButton, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, importCitedByButton, citedByProgress));

        //Create SplitPane to hold all nodes above
        SplitPane container = new SplitPane(citedByVBox, citingVBox);

        citingListView.prefHeightProperty().bind(container.heightProperty());
        citedByListView.prefHeightProperty().bind(container.heightProperty());

        styleFetchedListView(citingListView);
        styleFetchedListView(citedByListView);

        return container;
    }

    private void styleFetchedListView(CheckListView<CitationRelationItem> listView) {
        PseudoClass entrySelected = PseudoClass.getPseudoClass("entry-selected");
        new ViewModelListCellFactory<CitationRelationItem>()
                .withGraphic(e -> {

                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = getEntryNode(e.getBibEntry());
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox hContainer = new HBox();

                    if (e.isLocal()) {
                        Button jumpTo = IconTheme.JabRefIcons.LINK.asButton();

                        jumpTo.getStyleClass().add("addEntryButton");
                        jumpTo.setOnMouseClicked(event -> {
                            libraryTab.showAndEdit(e.getBibEntry());
                            citingTask.cancel();
                            citedByTask.cancel();
                        });
                        hContainer.getChildren().addAll(entryNode, separator, jumpTo);
                    } else {
                        ToggleButton addToggle = IconTheme.JabRefIcons.ADD.asToggleButton();
                        EasyBind.subscribe(addToggle.selectedProperty(), selected -> {
                            if (selected) {
                                addToggle.setGraphic(IconTheme.JabRefIcons.ADD_FILLED.withColor(IconTheme.SELECTED_COLOR).getGraphicNode());
                            } else {
                                addToggle.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
                            }
                        });
                        addToggle.getStyleClass().add("addEntryButton");
                        addToggle.selectedProperty().bindBidirectional(listView.getItemBooleanProperty(e));
                        hContainer.getChildren().addAll(entryNode, separator, addToggle);
                    }
                    hContainer.getStyleClass().add("entry-container");

                    if (citingListView.getItems().size() == 1) {
                        selectAllNewEntries(listView);
                    }

                    return hContainer;
                })
                .withOnMouseClickedEvent((ee, event) -> listView.getCheckModel().toggleCheckState(ee))
                .withPseudoClass(entrySelected, listView::getItemBooleanProperty)
                .install(listView);

        listView.setSelectionModel(new NoSelectionModel<>());
    }

    private Node getEntryNode(BibEntry entry) {
        Node entryType = getIcon(entry.getType()).getGraphicNode();
        entryType.getStyleClass().add("type");
        Label authors = new Label(entry.getFieldOrAliasLatexFree(StandardField.AUTHOR).orElse(""));
        authors.getStyleClass().add("authors");
        Label title = new Label(entry.getFieldOrAliasLatexFree(StandardField.TITLE).orElse(""));
        title.getStyleClass().add("title");
        Label year = new Label(entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(""));
        year.getStyleClass().add("year");
        Label journal = new Label(entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse(""));
        journal.getStyleClass().add("journal");

        VBox entryContainer = new VBox(
                new HBox(10, entryType, title),
                new HBox(5, year, journal),
                authors
        );
        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT).ifPresent(summaryText -> {
            TextFlowLimited summary = new TextFlowLimited(new Text(summaryText));
            summary.getStyleClass().add("summary");
            entryContainer.getChildren().add(summary);
        });

        entryContainer.getStyleClass().add("bibEntry");
        return entryContainer;
    }

    private IconTheme.JabRefIcons getIcon(EntryType type) {
        EnumSet<StandardEntryType> crossRefTypes = EnumSet.of(StandardEntryType.InBook, StandardEntryType.InProceedings, StandardEntryType.InCollection);
        if (type == StandardEntryType.Book) {
            return IconTheme.JabRefIcons.BOOK;
        } else if (crossRefTypes.contains(type)) {
            return IconTheme.JabRefIcons.OPEN_LINK;
        }
        return IconTheme.JabRefIcons.ARTICLE;
    }

    public void unselectAll(CheckListView<CitationRelationItem> listView) {
        listView.getCheckModel().clearChecks();
    }

    public void selectAllNewEntries(CheckListView<CitationRelationItem> listView) {
        unselectAll(listView);
        for (CitationRelationItem entry : listView.getItems()) {
            listView.getCheckModel().check(entry);
        }
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
     * @return boolean if tab should be shown
     */
    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.shouldShowCitationRelationsTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (preferences.isCitationRelationActivated()) {
            setContent(getPane(entry));
            searchForRelations(entry, citingListView, abortCitingButton, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, importCitingButton, citingProgress);
            searchForRelations(entry, citedByListView, abortCitedButton, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, importCitedByButton, citedByProgress);
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
    private void searchForRelations(BibEntry entry, CheckListView<CitationRelationItem> listView, Button abort, Button refreshButton, CitationRelationFetcher.SearchType searchType, Button importButton, ProgressIndicator progress) {
        //Check if current entry has DOI Number required for searching
        if (entry.getField(StandardField.DOI).isPresent()) {

            ObservableList<CitationRelationItem> observableList = FXCollections.observableArrayList();

            listView.getItems().clear();

            //Perform search in background and deal with success or failure
            List<BibEntry> localList = runOfflineTask(entry, searchType.equals(CitationRelationFetcher.SearchType.CITING) ? StandardField.CITING : StandardField.CITEDBY);
            if (!localList.isEmpty()) {
                for (BibEntry localAdd : localList) {
                    observableList.add(new CitationRelationItem(localAdd, true));
                }
            }
            listView.setItems(observableList);

            if (citingTask != null && !citingTask.isCanceled() && searchType.equals(CitationRelationFetcher.SearchType.CITING)) {
                citingTask.cancel();
            } else if (citedByTask != null && !citedByTask.isCanceled() && searchType.equals(CitationRelationFetcher.SearchType.CITEDBY)) {
                citedByTask.cancel();
            }

            CitationRelationFetcher fetcher = new CitationRelationFetcher(searchType);
            BackgroundTask<List<BibEntry>> task = BackgroundTask.wrap(() -> fetcher.performSearch(entry));

            if (searchType.equals(CitationRelationFetcher.SearchType.CITING)) {
                citingTask = task;
            } else {
                citedByTask = task;
            }

            task.onRunning(() -> {
                abort.setVisible(true);
                progress.setVisible(true);
                abort.setOnMouseClicked(event -> {
                    task.cancel();
                    abort.setVisible(false);
                    progress.setVisible(false);
                    dialogService.notify("Search aborted!");
                    refreshButton.setVisible(true);
                    importButton.setVisible(false);
                });
                refreshButton.setVisible(false);
                importButton.setVisible(false);
            })
                    .onSuccess(fetchedList -> {
                        abort.setVisible(false);
                        progress.setVisible(false);
                        if (!fetchedList.isEmpty()) {
                            filterDifference(fetchedList, observableList, searchType, entry);
                        }
                        if (!observableList.isEmpty()) {
                            listView.refresh();
                        } else {
                            Label placeholder = new Label("No articles found");
                            listView.setPlaceholder(placeholder);
                        }
                        BooleanBinding booleanBind = Bindings.isEmpty(listView.getCheckModel().getCheckedItems());
                        importButton.disableProperty().bind(booleanBind);
                        importButton.setOnMouseClicked(event -> importEntries(listView.getCheckModel().getCheckedItems(), searchType, entry));
                        refreshButton.setVisible(true);
                        importButton.setVisible(true);

                    })
                    .onFailure(exception -> {
                        LOGGER.error("Error while fetching citing Articles", exception);
                        abort.setVisible(false);
                        progress.setVisible(false);
                        dialogService.notify(exception.getMessage());
                        refreshButton.setVisible(true);
                        importButton.setVisible(false);
                    })
                    .executeWith(Globals.TASK_EXECUTOR);
        } else {
            dialogService.notify(Localization.lang("DOI-Number required, Please add DOI-Number to entry before searching."));
        }
    }

    /**
     * Performs a local lookup of Fields in StandardField.CITING/CITEDBY
     *
     * @param entry Current Entry Context
     * @param field The StandardField to work with
     */
    private List<BibEntry> runOfflineTask(BibEntry entry, StandardField field) {
        List<String> keys = getFilteredKeys(entry, field);
        List<BibEntry> list = new ArrayList<>();
        LOGGER.info("Current Keys/DOI in " + field.getName() + ":" + keys.toString());
        for (String key : keys) {
            BibEntry toAdd = getEntryByDOI(key);
            if (toAdd != null) {
                list.add(toAdd);
            }
        }
        return list;
    }

    /**
     * Filters an Observable List for Entries, that are already in the operator Field of the Entry.
     * If the entry is no duplicate, it also add the current entry to the negative operator of the entry in the List
     *
     * @param newEntries       The List to Filter
     * @param operator StandardField.CITING/CITED
     * @param entry    Current Entry Context
     */
    private void filterDifference(List<BibEntry> newEntries, ObservableList<CitationRelationItem> observableList, CitationRelationFetcher.SearchType operator, BibEntry entry) {
        StandardField field, nField;
        if (operator.equals(CitationRelationFetcher.SearchType.CITEDBY)) {
            field = StandardField.CITEDBY;
            nField = StandardField.CITING;
        } else {
            field = StandardField.CITING;
            nField = StandardField.CITEDBY;
        }
        List<String> currentKeys = getFilteredKeys(entry, field); // Current existant Enty.DOIs in Field
        for (BibEntry b : newEntries) {
            Optional<DOI> key = b.getDOI();
            Optional<DOI> entryKey = entry.getDOI();
            if (key.isPresent() && entryKey.isPresent()) { // Just Proceed if doi is present
                String doi = key.get().getDOI();
                String entryDoi = entryKey.get().getDOI();
                if (!currentKeys.contains(doi) && !doi_exists(doi)) { // if its not in the already referenced keys and not in the database = new Article
                    b.setField(nField, getFilteredKeys(b, nField) + "," + entryDoi);
                    observableList.add(new CitationRelationItem(b, false));
                } else {
                    if (!currentKeys.contains(doi)) { // if in database but not in keys
                        entry.setField(field, entry.getField(field).orElse("") + "," + doi);
                    }
                    // Add negative Reference to existing Entry and add this entry as local reference
                    BibEntry existing = getEntryByDOI(doi);
                    if (existing != null) {
                        existing.setField(nField, existing.getField(nField).orElse("") + "," + entryDoi);
                        observableList.add(new CitationRelationItem(existing, true));
                    }
                }
            }
        }
    }

    /**
     * Reads the CITING or CITED field, extracts the CitationKeys, checks whether the according entry still exists
     * in Database, set the new existing keys and return them
     *
     * @param entry    The Current selected Entry
     * @param operator StandardField.CITING/CITED
     * @return A List Containing the keys in the "operator"  field, theirs relations are in the Database
     */
    private List<String> getFilteredKeys(BibEntry entry, StandardField operator) {
        Optional<String> citingS = entry.getField(operator);
        if (citingS.isEmpty()) {
            LOGGER.info(entry.getCitationKey().orElse("no doi") + ": " + operator.getName() + " is empty!");
            return new ArrayList<>();
        }
        ArrayList<String> keys = new ArrayList<>(Arrays.asList(citingS.get().split(",")));
        filterNonExisting(keys);
        entry.setField(operator, String.join(",", keys));
        LOGGER.info(entry.getCitationKey().orElse("no doi") + ": " + operator.getName() + ": " + String.join(",", keys));
        return keys;
    }

    /**
     * Filters a given ArrayList of DOI's, whether they are in the Database
     *
     * @param toFilter The Arraylist to filter
     */
    private void filterNonExisting(ArrayList<String> toFilter) {
        toFilter.removeIf(s -> !this.doi_exists(s));
    }

    /**
     * Checks the current databasecontext whether an Entry with the given DOI exists
     * @param doi   The DOI to lookup as a String
     * @return DOI exists or not
     */
    private boolean doi_exists(String doi) {
        return !(getEntryByDOI(doi) == null);
    }

    /**
     * returns the Bibentry in the Database with the given DOI, or null if no such Entry exists
     * @param doi   doi TO LOOK for
     * @return null or found Entry
     */
    private BibEntry getEntryByDOI(String doi) {
        for (BibEntry b : databaseContext.getEntries()) {
            Optional<DOI> o = b.getDOI();
            if (o.isPresent() && o.get().getDOI().equals(doi)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Returns a String Containing the DOIs of a List of Entries. Ignores Entries with no DOI
     *
     * @param be The List of BibEntries to serialize
     * @return A Comma Separated List of CitationKeys(of the given List of Entries)
     */
    private String serialize(List<BibEntry> be) {
        List<String> ret = new ArrayList<>();
        for (BibEntry b : be) {
            Optional<DOI> s = b.getDOI();
            if (s.isPresent()) {
                String toAdd = s.get().getDOI(); // TODO: oder getNormalized() ?
                ret.add(toAdd);
            }
        }
        return String.join(",", ret);
    }

    /**
     * Function to import selected Entries to the Database. Also Writes the Entries to Import to the CITING/CITED Field
     *
     * @param entriesToImport entries to import
     */
    private void importEntries(List<CitationRelationItem> entriesToImport, CitationRelationFetcher.SearchType searchType, BibEntry entry) {
        citingTask.cancel();
        citedByTask.cancel();
        List<BibEntry> list = new ArrayList<>();
        for (CitationRelationItem item : entriesToImport) {
            list.add(item.getBibEntry());
        }
        ImportHandler importHandler = new ImportHandler(
                dialogService,
                databaseContext,
                ExternalFileTypes.getInstance(),
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager);
        importHandler.importEntries(list);
        if (searchType.equals(CitationRelationFetcher.SearchType.CITEDBY)) {
            entry.setField(StandardField.CITEDBY, serialize(list));
        } else {
            entry.setField(StandardField.CITING, serialize(list));
        }
        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }
}

