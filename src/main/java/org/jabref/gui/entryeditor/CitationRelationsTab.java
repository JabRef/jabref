package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

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
    private CheckListView<BibEntry> citingListView;
    private CheckListView<BibEntry> citedByListView;
    private CheckListView<BibEntry> citedByLocalListView;
    private CheckListView<BibEntry> citingLocalListView;
    private Button refreshCitingButton;
    private Button refreshCitedByButton;
    private Button importCitingButton;
    private Button importCitedByButton;
    private Button abortCitingButton;
    private Button abortCitedButton;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final PreferencesService preferencesService;
    private final LibraryTab libraryTab;

    //Tasks
    private BackgroundTask<List<BibEntry>> citingTask;
    private BackgroundTask<List<BibEntry>> citedByTask;


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
                setContent(getPane(entry));
                searchForRelations(entry, citingListView, citingLocalListView, abortCitingButton, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, importCitingButton);
                searchForRelations(entry, citedByListView, citedByLocalListView, abortCitedButton, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, importCitedByButton);
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
        AnchorPane citedByHBox = new AnchorPane();
        ScrollPane citingScrollPane = new ScrollPane();
        ScrollPane citedByScrollPane = new ScrollPane();
        VBox citingScrollVBox = new VBox();
        VBox citedByScrollVBox = new VBox();
        VBox citingInnerVbox = new VBox();
        VBox citedByInnerVbox = new VBox();

        //Create Heading Labels
        Label citingLabel = new Label("Citing");
        styleLabel(citingLabel);
        Label citedByLabel = new Label("Cited By");
        styleLabel(citedByLabel);

        //Create ListViews
        citingListView = new CheckListView<>();
        citedByListView = new CheckListView<>();
        citingLocalListView = new CheckListView<>();
        citedByLocalListView = new CheckListView<>();

        //Create refresh Buttons for both sides
        refreshCitingButton = IconTheme.JabRefIcons.REFRESH.asButton();
        styleTopBarButton(refreshCitingButton, 15.0);
        refreshCitedByButton = IconTheme.JabRefIcons.REFRESH.asButton();
        styleTopBarButton(refreshCitedByButton, 15.0);
        //Create abort Buttons for both sides
        abortCitingButton = IconTheme.JabRefIcons.CLOSE.asButton();
        styleTopBarButton(abortCitingButton, 15.0);
        abortCitedButton = IconTheme.JabRefIcons.CLOSE.asButton();
        styleTopBarButton(abortCitedButton, 15.0);

        //Create Import Buttons for both sides
        importCitingButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        styleTopBarButton(importCitingButton, 50.0);
        importCitedByButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        styleTopBarButton(importCitedByButton, 50.0);

        //Add nodes to parent layout containers
        citedByInnerVbox.getChildren().addAll(citedByLocalListView, citedByListView);
        citingInnerVbox.getChildren().addAll(citingLocalListView, citingListView);

        citingHBox.getChildren().addAll(abortCitingButton, citingLabel, refreshCitingButton, importCitingButton);
        citedByHBox.getChildren().addAll(abortCitedButton, citedByLabel, refreshCitedByButton, importCitedByButton);

        citingScrollPane.setContent(citingScrollVBox);
        citedByScrollPane.setContent(citedByScrollVBox);
        citingScrollVBox.getChildren().addAll(citingInnerVbox,new Label("Online"));
        citedByScrollVBox.getChildren().addAll(citedByInnerVbox, new Label("Online"));
        citingVBox.getChildren().addAll(citingHBox, citingScrollPane);
        citedByVBox.getChildren().addAll(citedByHBox, citedByScrollPane);

        refreshCitingButton.setOnMouseClicked(event -> searchForRelations(entry, citingListView, citingLocalListView, abortCitingButton, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, importCitingButton));
        refreshCitedByButton.setOnMouseClicked(event -> searchForRelations(entry, citedByListView, citedByLocalListView, abortCitedButton, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, importCitedByButton));
        abortCitingButton.setOnMouseClicked(event -> citingTask.cancel());
        abortCitedButton.setOnMouseClicked(event -> citedByTask.cancel());

        //Create SplitPane to hold all nodes above
        SplitPane container = new SplitPane(citedByVBox, citingVBox);

        citingScrollVBox.prefWidthProperty().bind(citingScrollPane.widthProperty());
        citedByScrollVBox.prefWidthProperty().bind(citedByScrollPane.widthProperty());

        styleFetchedListView(citingListView);
        styleFetchedListView(citedByListView);
        styleLocalListView(citedByLocalListView);
        styleLocalListView(citingLocalListView);

        return container;
    }

    private void styleLocalListView(CheckListView<BibEntry> listView) {
        new ViewModelListCellFactory<BibEntry>()
                .withGraphic(e -> {
                    Button jumpTo = IconTheme.JabRefIcons.LINK.asButton();

                    jumpTo.getStyleClass().add("addEntryButton");
                    jumpTo.setOnMouseClicked(event -> libraryTab.showAndEdit(e));
                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = getEntryNode(e);
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox hContainer = new HBox(entryNode, separator, jumpTo);
                    hContainer.getStyleClass().add("entry-container");

                    return hContainer;
                })
                .install(listView);
    }

    private void styleFetchedListView(CheckListView<BibEntry> listView) {
        PseudoClass entrySelected = PseudoClass.getPseudoClass("entry-selected");
        new ViewModelListCellFactory<BibEntry>()
                .withGraphic(e -> {
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
                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = getEntryNode(e);
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox hContainer = new HBox(entryNode, separator, addToggle);
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

    public void unselectAll(CheckListView<BibEntry> listView) {
        listView.getCheckModel().clearChecks();
    }

    public void selectAllNewEntries(CheckListView<BibEntry> listView) {
        unselectAll(listView);
        for (BibEntry entry : listView.getItems()) {
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
     * @param button button to style
     */
    private void styleTopBarButton(Button button, double offset) {
        button.setVisible(true);
        button.setAlignment(Pos.CENTER);
        AnchorPane.setTopAnchor(button, 0.0);
        AnchorPane.setBottomAnchor(button, 0.0);
        AnchorPane.setRightAnchor(button, offset);
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
            searchForRelations(entry, citingListView, citingLocalListView, abortCitingButton, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, importCitingButton);
            searchForRelations(entry, citedByListView, citedByLocalListView, abortCitedButton, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, importCitedByButton);
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
    private void searchForRelations(BibEntry entry, CheckListView<BibEntry> listView, CheckListView<BibEntry> localListView, Button abort, Button refreshButton, CitationRelationFetcher.SearchType searchType, Button importButton) {
        //Check if current entry has DOI Number required for searching
        if (entry.getField(StandardField.DOI).isPresent()) {
            //Perform search in background and deal with success or failure
            if (searchType.equals(CitationRelationFetcher.SearchType.CITING)) {
                runOfflineTask(entry, localListView, StandardField.CITING);
                runOnlineTask(citingTask, entry, listView, abort, refreshButton, searchType, importButton);
            } else {
                runOfflineTask(entry, localListView, StandardField.CITEDBY);
                runOnlineTask(citedByTask, entry, listView, abort, refreshButton, searchType, importButton);
            }
        } else {
            dialogService.notify(Localization.lang("DOI-Number required, Please add DOI-Number to entry before searching."));
        }
    }

    private void runOnlineTask(BackgroundTask<List<BibEntry>> task, BibEntry entry, CheckListView<BibEntry> listView, Button abort, Button refreshButton, CitationRelationFetcher.SearchType searchType, Button importButton) {
        //Create ObservableList to display in ListView
        ObservableList<BibEntry> observableList = FXCollections.observableArrayList();

        //New Instance of CitationRelationsFetcher with correct searchType
        CitationRelationFetcher fetcher = new CitationRelationFetcher(searchType);
        task = BackgroundTask.wrap(() -> fetcher.performSearch(entry));

        task.onRunning(() -> {
            listView.getItems().clear();
            listView.setVisible(false);
            abort.setVisible(true);
            refreshButton.setVisible(false);
            importButton.setVisible(false);
        })
            .onSuccess(fetchedList -> {
                abort.setVisible(false);
                observableList.addAll(fetchedList);
                filterDifference(observableList, searchType, entry);
                if (observableList.isEmpty()) {
                    Label placeholder = new Label("No articles found");
                    listView.setPlaceholder(placeholder);
                }
                listView.setItems(observableList);
                listView.setVisible(true);
                refreshButton.setVisible(true);
                importButton.setVisible(true);
                BooleanBinding booleanBind = Bindings.isEmpty(listView.getCheckModel().getCheckedItems());
                importButton.disableProperty().bind(booleanBind);

                importButton.setOnMouseClicked(event -> importEntries(listView.getCheckModel().getCheckedItems(), searchType, entry));
            })
            .onFailure(exception -> {
                LOGGER.error("Error while fetching citing Articles", exception);
                abort.setVisible(false);
                dialogService.notify(exception.getMessage());
                refreshButton.setDisable(false);
                importButton.setVisible(false);
            })
            .executeWith(Globals.TASK_EXECUTOR);
    }

    /**
     * Performs a local lookup of Fields in StandardField.CITING/CITEDBY
     * @param entry         Current Entry Context
     * @param localListView The Listview that should Contain the Results
     * @param field         The StandardField to work with
     */
    private void runOfflineTask(BibEntry entry, ListView<BibEntry> localListView, StandardField field) {
        List<String> keys = getFilteredKeys(entry, field);
        ObservableList<BibEntry> observableList = FXCollections.observableArrayList();

        for (String key : keys) {
            Optional<BibEntry> o = databaseContext.getDatabase().getEntryByCitationKey(key);
            o.ifPresent(observableList::add);
        }
        if (observableList.isEmpty()) {
            localListView.setPlaceholder(new Label("No Local Entries Found!"));
        } else {
            localListView.setItems(observableList);
        }
    }

    /**
     * Filters an Observable List for Entries, that are already in the operator Field of the Entry.
     * @param ol        The List to Filter
     * @param operator  StandardField.CITING/CITED
     * @param entry     Current Entry Context
     */
    private void filterDifference(ObservableList<BibEntry> ol, CitationRelationFetcher.SearchType operator, BibEntry entry) {
        StandardField field;
        if (operator.equals(CitationRelationFetcher.SearchType.CITEDBY)) {
            field = StandardField.CITEDBY;
        } else {
            field = StandardField.CITING;
        }
        CitationKeyGenerator ckg = new CitationKeyGenerator(databaseContext, Globals.prefs.getCitationKeyPatternPreferences());
        List<String> currentKeys = getFilteredKeys(entry, field);
        for (BibEntry b : ol) {
            ckg.generateAndSetKey(b);
            if (currentKeys.contains(b.getCitationKey().orElse(""))) {
                ol.remove(b);
            }
        }
    }

    /**
     * Reads the CITING or CITED field, extracts the CitationKeys, checks whether the according entry still exists
     * in Database, set the new existing keys and return them
     * @param entry The Current selected Entry
     * @param operator StandardField.CITING/CITED
     * @return
     */
    private List<String> getFilteredKeys(BibEntry entry, StandardField operator) {
        String citingS = entry.getField(operator).orElse("");
        ArrayList<String> keys = new ArrayList<>(Arrays.asList(citingS.split(",")));
        filterNonExisting(keys);
        entry.setField(operator, keys.toString());
        return keys;
    }

    /**
     * Filters a given ArrayList of Citationkeys, whether they are in the Database
     * @param toFilter The Arraylist to filter
     */
    private void filterNonExisting(ArrayList<String> toFilter) {
        toFilter.removeIf(s -> databaseContext.getDatabase().getEntryByCitationKey(s).isEmpty());
    }

    /**
     * Returns a String Containing the CitationKeys of a List of Entries
     */
    private String serialize(List<BibEntry> be) {
        List<String> ret = new ArrayList<>();
        for (BibEntry b: be) {
            Optional<String> s = b.getCitationKey();
            s.ifPresent(ret::add);
        }
        return ret.toString();
    }

    /**
     * Function to import selected Entries to the Database
     * @param entriesToImport entries to import
     */
    private void importEntries(List<BibEntry> entriesToImport, CitationRelationFetcher.SearchType searchType, BibEntry entry) {
        ImportHandler importHandler = new ImportHandler(
                dialogService,
                databaseContext,
                ExternalFileTypes.getInstance(),
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager);
        importHandler.importEntries(entriesToImport);
        if (searchType.equals(CitationRelationFetcher.SearchType.CITEDBY)) {
            entry.setField(StandardField.CITEDBY, serialize(entriesToImport));
        } else {
            entry.setField(StandardField.CITING, serialize(entriesToImport));
        }
        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }
}

