package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelatedEntriesComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntriesComponent.class);
    private final RelatedEntriesRepository repository;
    private final LibraryTab libraryTab;
    private final RelatedEntriesComponentConfig config;
    private final RelatedEntriesComponentViewModel viewModel;
    private Button refreshButton;
    private Button cancelButton;
    private ProgressIndicator progressIndicator;
    private Button importEntriesButton;
    private CheckListView<CitationRelationItem> relatedEntriesListView;

    public RelatedEntriesComponent(BibEntry pivotEntry, RelatedEntriesRepository repository, LibraryTab libraryTab,
                                   RelatedEntriesComponentConfig config) {
        this.config = config;
        this.viewModel = new RelatedEntriesComponentViewModel(pivotEntry, repository);
        this.repository = repository;
        this.libraryTab = libraryTab;

        initUI();
        viewModel.loadEntries();

        viewModel.relatedEntriesResultPropertyProperty().addListener((observable, oldValue, result) -> {
            if (result.isPending()) {
                showNodes(progressIndicator, cancelButton);
                hideNodes(refreshButton, importEntriesButton);
            } else if (result.isSuccess()) {
                hideNodes(progressIndicator, cancelButton);
                showNodes(refreshButton, importEntriesButton);

                List<BibEntry> entries = result.asSuccess().value();
                if (entries.isEmpty()) {
                    relatedEntriesListView.setPlaceholder(buildLabel(Localization.lang("No articles found")));
                } else {
                    relatedEntriesListView.setItems(
                            FXCollections.observableArrayList(entries.stream().map(entry ->
                                    new CitationRelationItem(entry, false)).collect(Collectors.toList()))
                    );
                }
            } else if (result.isFailure()) {
                hideNodes(progressIndicator, cancelButton);
                showNodes(refreshButton, importEntriesButton);

                Exception exception = result.asFailure().exception();

                relatedEntriesListView.setPlaceholder(buildLabel(
                        Localization.lang("Error while fetching citing entries: %0", exception.getMessage())));

                LOGGER.error("Error while fetching entry's citation relations", exception);
            } else {
                throw new IllegalStateException("Result is can be in one of the three legal states: PENDING, SUCCESS or FAILURE");
            }
        });
    }

    private void initUI () {
        // Create Layout Containers
        this.setFillWidth(true);
        this.setAlignment(Pos.TOP_CENTER);

        // Create Heading Lab
        Label headingLabel = new Label(Localization.lang(config.getHeading()));
        headingLabel.setStyle("-fx-padding: 5px");
        headingLabel.setAlignment(Pos.CENTER);
        AnchorPane.setTopAnchor(headingLabel, 0.0);
        AnchorPane.setLeftAnchor(headingLabel, 0.0);
        AnchorPane.setBottomAnchor(headingLabel, 0.0);
        AnchorPane.setRightAnchor(headingLabel, 0.0);

        relatedEntriesListView = new CheckListView<>();
        styleRelatedEntriesListView();
        VBox.setVgrow(relatedEntriesListView, Priority.ALWAYS);

        refreshButton = IconTheme.JabRefIcons.REFRESH.asButton();
        refreshButton.setTooltip(new Tooltip(Localization.lang("Restart search")));
        styleTopBarNode(refreshButton, 15.0);
        refreshButton.setOnAction(e -> viewModel.reloadEntries());

        cancelButton = IconTheme.JabRefIcons.CLOSE.asButton();
        cancelButton.getGraphic().resize(30, 30);
        cancelButton.setTooltip(new Tooltip(Localization.lang("Cancel search")));
        styleTopBarNode(cancelButton, 15.0);
        cancelButton.setOnAction(e -> viewModel.cancelLoading());


        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(25, 25);
        styleTopBarNode(progressIndicator, 50.0);

        // Create import buttons for both sides
        importEntriesButton = IconTheme.JabRefIcons.ADD_ENTRY.asButton();
        importEntriesButton.setTooltip(new Tooltip(Localization.lang("Add selected entries to database")));
        styleTopBarNode(importEntriesButton, 50.0);

        AnchorPane topbar = new AnchorPane();
        topbar.setPrefHeight(40);
        topbar.getChildren().addAll(headingLabel, refreshButton,
                importEntriesButton, progressIndicator, cancelButton);

        this.getChildren().addAll(topbar, relatedEntriesListView);
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

    private void styleRelatedEntriesListView() {
        PseudoClass entrySelected = PseudoClass.getPseudoClass("selected");
        new ViewModelListCellFactory<CitationRelationItem>()
                .withGraphic(entry -> {
                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = BibEntryView.getEntryNode(entry.getEntry());
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox hContainer = new HBox();
                    hContainer.prefWidthProperty().bind(relatedEntriesListView.widthProperty().subtract(25));

                    if (entry.isLocal()) {
                        Button jumpTo = IconTheme.JabRefIcons.LINK.asButton();
                        jumpTo.setTooltip(new Tooltip(Localization.lang("Jump to entry in database")));
                        jumpTo.getStyleClass().add("addEntryButton");
                        jumpTo.setOnMouseClicked(event -> {
                            libraryTab.showAndEdit(entry.getEntry());
                            libraryTab.clearAndSelect(entry.getEntry());
                            viewModel.cancelLoading();
                        });
                        hContainer.getChildren().addAll(entryNode, separator, jumpTo);
                    } else {
                        ToggleButton addToggle = IconTheme.JabRefIcons.ADD.asToggleButton();
                        addToggle.setTooltip(new Tooltip(Localization.lang("Select entry")));
                        EasyBind.subscribe(addToggle.selectedProperty(), selected -> {
                            if (selected) {
                                addToggle.setGraphic(IconTheme.JabRefIcons.ADD_FILLED
                                        .withColor(IconTheme.SELECTED_COLOR).getGraphicNode());
                            } else {
                                addToggle.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
                            }
                        });
                        addToggle.getStyleClass().add("addEntryButton");
                        addToggle.selectedProperty().bindBidirectional(relatedEntriesListView.getItemBooleanProperty(entry));
                        hContainer.getChildren().addAll(entryNode, separator, addToggle);
                    }
                    hContainer.getStyleClass().add("entry-container");

                    return hContainer;
                })
                .withOnMouseClickedEvent((ee, event) -> {
                    if (!ee.isLocal()) {
                        relatedEntriesListView.getCheckModel().toggleCheckState(ee);
                    }
                })
                .withPseudoClass(entrySelected, relatedEntriesListView::getItemBooleanProperty)
                .install(relatedEntriesListView);

        relatedEntriesListView.setSelectionModel(new NoSelectionModel<>());
    }

    private void hideNodes(Node... nodes) {
        Arrays.stream(nodes).forEach(node -> node.setVisible(false));
    }

    private void showNodes(Node... nodes) {
        Arrays.stream(nodes).forEach(node -> node.setVisible(true));
    }

    private Label buildLabel(String text) {
        return new Label(text);
    }
/*
    *//**
     * Function to import selected entries to the database. Also writes the entries to import to the CITING/CITED field
     *
     * @param entriesToImport entries to import
     *//*
    private void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry entry) {
        citingTask.cancel();
        citedByTask.cancel();
        List<BibEntry> entries = entriesToImport.stream().map(CitationRelationItem::getEntry).collect(Collectors.toList());
        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                Globals.TASK_EXECUTOR);
        importHandler.importEntries(entries);

        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }*/

}
