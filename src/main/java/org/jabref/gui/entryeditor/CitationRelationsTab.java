package org.jabref.gui.entryeditor;

import java.util.EnumSet;
import java.util.List;

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
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.TextFlowLimited;
import org.jabref.gui.util.ViewModelListCellFactory;
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
    private StackPane citingStackPane;
    private StackPane citedByStackPane;
    private Button startCitingButton;
    private Button startCitedByButton;
    private Button refreshCitingButton;
    private Button refreshCitedByButton;
    private Button importCitingButton;
    private Button importCitedByButton;
    private Label errorLabel;
    private Label citingProgressLabel;
    private Label citedByProgressLabel;
    private ProgressIndicator citingProgress;
    private ProgressIndicator citedByProgress;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final PreferencesService preferencesService;

    public CitationRelationsTab(EntryEditorPreferences preferences, DialogService dialogService, BibDatabaseContext databaseContext, UndoManager undoManager, StateManager stateManager, FileUpdateMonitor fileUpdateMonitor, PreferencesService preferencesService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.preferencesService = preferencesService;
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));

    }

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
                searchForRelations(entry, startCitingButton, citingListView, citingProgress, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, citingStackPane, citingProgressLabel, importCitingButton);
                searchForRelations(entry, startCitedByButton, citedByListView, citedByProgress, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, citedByStackPane, citedByProgressLabel, importCitedByButton);
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
        citingStackPane = new StackPane();
        citedByStackPane = new StackPane();

        //Create Heading Labels
        Label citingLabel = new Label("Citing");
        styleLabel(citingLabel);
        Label citedByLabel = new Label("Cited By");
        styleLabel(citedByLabel);

        //Create ListViews
        citingListView = new CheckListView<>();
        citedByListView = new CheckListView<>();

        //Create Start Buttons
        startCitingButton = new Button();
        startCitingButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.SEARCH));
        startCitedByButton = new Button();
        startCitedByButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.SEARCH));

        //Create refresh Buttons for both sides
        refreshCitingButton = IconTheme.JabRefIcons.REFRESH.asButton();
        styleTopBarButton(refreshCitingButton, 15.0);
        refreshCitedByButton = IconTheme.JabRefIcons.REFRESH.asButton();
        styleTopBarButton(refreshCitedByButton, 15.0);

        //Create refresh Buttons for both sides
        importCitingButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        styleTopBarButton(importCitingButton, 50.0);
        importCitedByButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        styleTopBarButton(importCitedByButton, 50.0);

        //Create Progress Indicators
        citingProgress = new ProgressIndicator();
        citingProgress.setMaxSize(60, 60);
        citingProgress.setVisible(false);
        citedByProgress = new ProgressIndicator();
        citedByProgress.setMaxSize(60, 60);
        citedByProgress.setVisible(false);
        citingProgressLabel = new Label();
        citedByProgressLabel = new Label();

        //Add nodes to parent layout containers
        citedByStackPane.getChildren().addAll(citedByListView, startCitedByButton, citedByProgress, citedByProgressLabel);
        citingStackPane.getChildren().addAll(citingListView, startCitingButton, citingProgress, citingProgressLabel);
        citingHBox.getChildren().addAll(citingLabel, refreshCitingButton, importCitingButton);
        citedByHBox.getChildren().addAll(citedByLabel, refreshCitedByButton, importCitedByButton);
        citingVBox.getChildren().addAll(citingHBox, citingStackPane);
        citedByVBox.getChildren().addAll(citedByHBox, citedByStackPane);

        refreshCitingButton.setOnMouseClicked(event -> searchForRelations(entry, startCitingButton, citingListView, citingProgress, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, citingStackPane, citingProgressLabel, importCitingButton));
        refreshCitedByButton.setOnMouseClicked(event -> searchForRelations(entry, startCitedByButton, citedByListView, citedByProgress, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, citedByStackPane, citedByProgressLabel, importCitedByButton));

        //Create SplitPane to hold all nodes above
        SplitPane container = new SplitPane(citedByVBox, citingVBox);

        errorLabel = new Label();

        citingListView.prefHeightProperty().bind(container.heightProperty().subtract(25));
        citedByListView.prefHeightProperty().bind(container.heightProperty().subtract(25));

        styleListView(citingListView);
        styleListView(citedByListView);

        return container;
    }

    private void styleListView(CheckListView<BibEntry> listView) {
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
        button.setVisible(false);
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
            searchForRelations(entry, startCitingButton, citingListView, citingProgress, refreshCitingButton, CitationRelationFetcher.SearchType.CITING, citingStackPane, citingProgressLabel, importCitingButton);
            searchForRelations(entry, startCitedByButton, citedByListView, citedByProgress, refreshCitedByButton, CitationRelationFetcher.SearchType.CITEDBY, citedByStackPane, citedByProgressLabel, importCitedByButton);
        } else {
            setContent(getActivationPane(entry));
        }
    }

    /**
     * Method to start search for relations and display them
     * in the associated ListView
     *
     * @param entry         BibEntry currently selected in Jabref Database
     * @param startButton   start button to use
     * @param listView      ListView to use
     * @param progress      ProgressIndicator to use
     * @param refreshButton refresh Button to use
     * @param searchType    type of search (CITING / CITEDBY)
     * @param stackPane     StackPane to use
     */
    private void searchForRelations(BibEntry entry, Button startButton, CheckListView<BibEntry> listView, ProgressIndicator progress, Button refreshButton, CitationRelationFetcher.SearchType searchType, StackPane stackPane, Label progressLabel, Button importButton) {
        //Check if current entry has DOI Number required for searching
        if (entry.getField(StandardField.DOI).isPresent()) {
            if (entry.getField(StandardField.CITED).isPresent()) {

            }

            //Create ObservableList to display in ListView
            ObservableList<BibEntry> observableList = FXCollections.observableArrayList();
            startButton.setVisible(false);

            //New Instance of CitationRelationsFetcher with correct searchType
            CitationRelationFetcher fetcher = new CitationRelationFetcher(searchType, progressLabel);

            //Perform search in background and deal with success or failure
            BackgroundTask
                    .wrap(() -> fetcher.performSearch(entry))
                    .onRunning(() -> {
                        listView.getItems().clear();
                        progress.setVisible(true);
                        refreshButton.setVisible(false);
                        importButton.setVisible(false);
                        stackPane.getChildren().remove(errorLabel);
                        progressLabel.setVisible(true);
                    })
                    .onSuccess(fetchedList -> {
                        progress.setVisible(false);
                        progressLabel.setVisible(false);
                        observableList.addAll(fetchedList);
                        if (observableList.isEmpty()) {
                            Label placeholder = new Label("No articles found");
                            listView.setPlaceholder(placeholder);
                        }
                        listView.setItems(observableList);
                        refreshButton.setVisible(true);
                        importButton.setVisible(true);
                        BooleanBinding booleanBind = Bindings.isEmpty(listView.getCheckModel().getCheckedItems());
                        importButton.disableProperty().bind(booleanBind);

                        importButton.setOnMouseClicked(event -> importEntries(listView.getCheckModel().getCheckedItems()));
                    })
                    .onFailure(exception -> {
                        LOGGER.error("Error while fetching citing Articles", exception);
                        progress.setVisible(false);
                        progressLabel.setVisible(false);
                        errorLabel.setText(exception.getMessage());
                        stackPane.getChildren().add(errorLabel);
                        refreshButton.setVisible(true);
                        importButton.setVisible(true);
                    })
                    .executeWith(Globals.TASK_EXECUTOR);
        } else {
            dialogService.notify(Localization.lang("DOI-Number required, Please add DOI-Number to entry before searching."));
        }
    }

    private void importEntries(List<BibEntry> entriesToImport) {
        ImportHandler importHandler = new ImportHandler(
                dialogService,
                databaseContext,
                ExternalFileTypes.getInstance(),
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager);
        importHandler.importEntries(entriesToImport);
        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }
}

