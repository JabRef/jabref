package org.jabref.gui.entryeditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.fetcher.CitationRelationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI for tab displaying an articles citation relations based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(org.jabref.gui.entryeditor.CitationRelationsTab.class);
    private final EntryEditorPreferences preferences;
    private final EntryEditor entryEditor;
    private final DialogService dialogService;
    private VBox citingVBox;
    private VBox citedByVBox;
    private HBox citingHBox;
    private HBox citedByHBox;
    private Label citingLabel;
    private Label citedByLabel;
    private ListView<BibEntry> citingListView;
    private ListView<BibEntry> citedByListView;
    private StackPane citingStackPane;
    private StackPane citedByStackPane;
    private Button startCitingButton;
    private Button startCitedByButton;
    private Button refreshCitingButton;
    private Button refreshCitedByButton;
    private ProgressIndicator citingProgress;
    private ProgressIndicator citedByProgress;

    public CitationRelationsTab(EntryEditor entryEditor, EntryEditorPreferences preferences, DialogService dialogService) {
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));
        this.entryEditor = entryEditor;
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    private SplitPane getPane(BibEntry entry) {
        citingVBox = new VBox();
        citedByVBox = new VBox();
        citingHBox = new HBox();
        citedByHBox = new HBox();
        citingLabel = new Label("Citing");
        citingLabel.setStyle("-fx-padding: 5px");
        citedByLabel = new Label("Cited By");
        citedByLabel.setStyle("-fx-padding: 5px");
        citingListView = new ListView<>();
        citingListView.setCellFactory(listView -> new CitationRelationListCell());
        citingListView.setOnMouseClicked(event -> onCitingListClicked());
        citedByListView = new ListView<>();
        citedByListView.setCellFactory(listView -> new CitationRelationListCell());
        citedByListView.setOnMouseClicked(event -> onCitedByListClicked());
        citingStackPane = new StackPane();
        citedByStackPane = new StackPane();
        startCitingButton = new Button("Search");
        startCitedByButton = new Button("Search");
        refreshCitingButton = new Button();
        refreshCitingButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.REFRESH));
        refreshCitingButton.setVisible(false);
        refreshCitedByButton = new Button();
        refreshCitedByButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.REFRESH));
        refreshCitedByButton.setVisible(false);

        citedByStackPane.getChildren().add(citedByListView);
        citingStackPane.getChildren().add(citingListView);
        citedByStackPane.getChildren().add(startCitedByButton);
        citingStackPane.getChildren().add(startCitingButton);
        citingProgress = new ProgressIndicator();
        citingProgress.setMaxSize(50, 50);
        citingProgress.setVisible(false);
        citedByProgress = new ProgressIndicator();
        citedByProgress.setMaxSize(50, 50);
        citedByProgress.setVisible(false);
        citingStackPane.getChildren().add(citingProgress);
        citedByStackPane.getChildren().add(citedByProgress);

        citingVBox.setFillWidth(true);
        citedByVBox.setFillWidth(true);
        citingHBox.setAlignment(Pos.CENTER);
        citedByHBox.setAlignment(Pos.CENTER);

        citingHBox.getChildren().add(citingLabel);
        citedByHBox.getChildren().add(citedByLabel);
        citingHBox.getChildren().add(refreshCitingButton);
        citedByHBox.getChildren().add(refreshCitedByButton);

        citingVBox.getChildren().add(citingHBox);
        citingVBox.getChildren().add(citingStackPane);
        citedByVBox.getChildren().add(citedByHBox);
        citedByVBox.getChildren().add(citedByStackPane);
        citingVBox.setAlignment(Pos.TOP_CENTER);
        citedByVBox.setAlignment(Pos.TOP_CENTER);

        startCitingButton.setOnMouseClicked(event -> citingClicked(entry));
        startCitedByButton.setOnMouseClicked(event -> citedByClicked(entry));
        refreshCitingButton.setOnMouseClicked(event -> citingClicked(entry));
        refreshCitedByButton.setOnMouseClicked(event -> citedByClicked(entry));

        SplitPane container = new SplitPane(citingVBox, citedByVBox);
        setContent(container);

        return container;
    }

    private void onCitedByListClicked() {
        if (!citedByListView.getItems().isEmpty()) {
            System.out.println("Clicked on: " + citedByListView.getSelectionModel().getSelectedItem().getField(StandardField.TITLE));
        }
    }

    private void onCitingListClicked() {
        if (!citingListView.getItems().isEmpty()) {
            System.out.println("Clicked on: " + citingListView.getSelectionModel().getSelectedItem().getField(StandardField.TITLE));
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.shouldShowCitationRelationsTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        setContent(getPane(entry));
    }

    private void citingClicked(BibEntry entry) {
        ObservableList<BibEntry> citingList = FXCollections.observableArrayList();
        startCitingButton.setVisible(false);
        CitationRelationFetcher fetcher = new CitationRelationFetcher(CitationRelationFetcher.SearchType.CITING, citingListView);
        BackgroundTask
                .wrap(() -> fetcher.performSearch(entry))
                .onRunning(() -> {
                    citingListView.getItems().clear();
                    citingProgress.setVisible(true);
                    refreshCitingButton.setVisible(false);
                })
                .onSuccess(fetchedList -> {
                    citingProgress.setVisible(false);
                    citingList.addAll(fetchedList);
                    if (citingList.isEmpty()) {
                        BibEntry emptyEntry = new BibEntry();
                        emptyEntry.setField(StandardField.TITLE, "No Articles Found");
                        emptyEntry.setField(StandardField.AUTHOR, "-");
                        citingList.add(emptyEntry);
                    }
                    citingListView.setItems(citingList);
                    refreshCitingButton.setVisible(true);
                })
                .onFailure(exception -> {
                    LOGGER.error("Error while fetching citing Articles", exception);
                    citingProgress.setVisible(false);
                    citingStackPane.getChildren().add(new Label("Error"));
                    refreshCitingButton.setVisible(true);
                })
                .executeWith(Globals.TASK_EXECUTOR);
    }

    private void citedByClicked(BibEntry entry) {
        ObservableList<BibEntry> citedByList = FXCollections.observableArrayList();
        startCitedByButton.setVisible(false);
        CitationRelationFetcher fetcher = new CitationRelationFetcher(CitationRelationFetcher.SearchType.CITEDBY, citedByListView);
        BackgroundTask
                .wrap(() -> fetcher.performSearch(entry))
                .onRunning(() -> {
                    citedByListView.getItems().clear();
                    citedByProgress.setVisible(true);
                    refreshCitedByButton.setVisible(false);
                })
                .onSuccess(fetchedList -> {
                    citedByProgress.setVisible(false);
                    citedByList.addAll(fetchedList);
                    if (citedByList.isEmpty()) {
                        BibEntry emptyEntry = new BibEntry();
                        emptyEntry.setField(StandardField.TITLE, "No Articles Found");
                        emptyEntry.setField(StandardField.AUTHOR, "-");
                        citedByList.add(emptyEntry);
                    }
                    citedByListView.setItems(citedByList);
                    refreshCitedByButton.setVisible(true);
                })
                .onFailure(exception -> {
                    LOGGER.error("Error while fetching citing Articles", exception);
                    citedByProgress.setVisible(false);
                    citedByStackPane.getChildren().add(new Label("Error"));
                    refreshCitedByButton.setVisible(true);
                })
                .executeWith(Globals.TASK_EXECUTOR);
    }
}
