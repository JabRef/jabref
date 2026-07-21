package org.jabref.gui.preferences.journals;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ColorUtil;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.textfield.CustomTextField;

/// Tab for managing journal abbreviation lists.
public class JournalAbbreviationsTab extends AbstractPreferenceTabView<JournalAbbreviationsTabViewModel> {

    private final TableView<AbbreviationViewModel> journalAbbreviationsTable = new TableView<>();
    private final TableColumn<AbbreviationViewModel, String> journalTableNameColumn = new TableColumn<>(Localization.lang("Full journal name"));
    private final CustomTextField searchBox = new CustomTextField();

    private final FilteredList<AbbreviationViewModel> filteredAbbreviations;

    private Timeline invalidateSearch;

    public JournalAbbreviationsTab() {
        viewModel = new JournalAbbreviationsTabViewModel(
                preferences.getAbbreviationPreferences(),
                dialogService,
                taskExecutor,
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
        filteredAbbreviations = new FilteredList<>(viewModel.abbreviationsProperty());

        buildView();
        setAnimations();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Journal abbreviations");
    }

    private void buildView() {
        getChildren().add(form()
                .custom(buildFileChooserRow())
                .custom(buildAbbreviationsEditor())
                .custom(buildAddAbbreviationRow())
                .checkbox(Localization.lang("Use the field FJournal to store the full journal name for (un)abbreviations in the entry"), viewModel.useFJournalProperty())
                .build());
    }

    private Node buildFileChooserRow() {
        ComboBox<AbbreviationsFileViewModel> journalFilesBox = new ComboBox<>();
        journalFilesBox.setPromptText(Localization.lang("No abbreviation files loaded"));
        journalFilesBox.setMinWidth(200.0);
        journalFilesBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(journalFilesBox, Priority.ALWAYS);
        journalFilesBox.itemsProperty().bindBidirectional(viewModel.journalFilesProperty());
        journalFilesBox.valueProperty().bindBidirectional(viewModel.currentFileProperty());

        Button addList = toolButton(IconTheme.JabRefIcons.ADD_ABBREVIATION_LIST, Localization.lang("Add new list"), viewModel::addNewFile);
        Button openList = toolButton(IconTheme.JabRefIcons.OPEN_ABBREVIATION_LIST, Localization.lang("Open existing list"), viewModel::openFile);
        Button removeList = toolButton(IconTheme.JabRefIcons.REMOVE_ABBREVIATION_LIST, Localization.lang("Remove list"), viewModel::removeCurrentFile);
        removeList.disableProperty().bind(viewModel.isFileRemovableProperty().not());

        HBox row = new HBox(4.0, new Label(Localization.lang("Journal lists:")), journalFilesBox, addList, openList, removeList);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button toolButton(IconTheme.JabRefIcons icon, String tooltip, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "narrow");
        button.setGraphic(icon.getGraphicNode());
        Tooltip tip = new Tooltip(tooltip);
        tip.setAutoHide(true);
        button.setTooltip(tip);
        button.setOnAction(_ -> action.run());
        return button;
    }

    private Node buildAbbreviationsEditor() {
        searchBox.setPromptText(Localization.lang("Filter"));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        VBox.setMargin(searchBox, new Insets(3.0));
        searchBox.textProperty().addListener((_, _, searchTerm) ->
                filteredAbbreviations.setPredicate(abbreviation -> searchTerm.isEmpty() || abbreviation.containsCaseIndependent(searchTerm)));

        setUpTable();
        setBindings();

        return new VBox(10.0, searchBox, journalAbbreviationsTable);
    }

    private void setUpTable() {
        journalAbbreviationsTable.setEditable(true);
        journalAbbreviationsTable.getStyleClass().add("abbreviations-table");
        journalAbbreviationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        journalTableNameColumn.setPrefWidth(400.0);
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<AbbreviationViewModel, String> abbreviationColumn = new TableColumn<>(Localization.lang("Abbreviation name"));
        abbreviationColumn.setPrefWidth(200.0);
        abbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        abbreviationColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<AbbreviationViewModel, String> shortestUniqueAbbreviationColumn = new TableColumn<>(Localization.lang("Shortest unique abbreviation"));
        shortestUniqueAbbreviationColumn.setPrefWidth(200.0);
        shortestUniqueAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().shortestUniqueAbbreviationProperty());
        shortestUniqueAbbreviationColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<AbbreviationViewModel, String> actionsColumn = new TableColumn<>();
        actionsColumn.setMinWidth(30.0);
        actionsColumn.setMaxWidth(30.0);
        actionsColumn.setPrefWidth(30.0);
        actionsColumn.setResizable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<AbbreviationViewModel, String>()
                .withGraphic(_ -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove journal '%0'", name))
                .withDisableExpression(_ -> viewModel.isEditableAndRemovableProperty().not())
                .withVisibleExpression(_ -> viewModel.isEditableAndRemovableProperty())
                .withOnMouseClickedEvent(_ -> _ ->
                        viewModel.removeAbbreviation(journalAbbreviationsTable.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        journalAbbreviationsTable.getColumns().add(journalTableNameColumn);
        journalAbbreviationsTable.getColumns().add(abbreviationColumn);
        journalAbbreviationsTable.getColumns().add(shortestUniqueAbbreviationColumn);
        journalAbbreviationsTable.getColumns().add(actionsColumn);

        journalTableNameColumn.editableProperty().bind(viewModel.isAbbreviationEditableAndRemovable());
        abbreviationColumn.editableProperty().bind(viewModel.isAbbreviationEditableAndRemovable());
        shortestUniqueAbbreviationColumn.editableProperty().bind(viewModel.isAbbreviationEditableAndRemovable());

        Label loadingLabel = new Label(Localization.lang("Loading built in lists"));
        loadingLabel.setMaxHeight(30.0);
        loadingLabel.visibleProperty().bind(viewModel.isLoadingProperty());
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxHeight(30.0);
        progressIndicator.setOpacity(0.4);
        progressIndicator.visibleProperty().bind(viewModel.isLoadingProperty());
        journalAbbreviationsTable.setPlaceholder(new StackPane(loadingLabel, progressIndicator));
    }

    private void setBindings() {
        journalAbbreviationsTable.setItems(filteredAbbreviations);

        EasyBind.subscribe(journalAbbreviationsTable.getSelectionModel().selectedItemProperty(), newValue ->
                viewModel.currentAbbreviationProperty().set(newValue));
        EasyBind.subscribe(viewModel.currentAbbreviationProperty(), newValue ->
                journalAbbreviationsTable.getSelectionModel().select(newValue));
    }

    private Node buildAddAbbreviationRow() {
        Button addAbbreviationButton = new Button(Localization.lang("Add abbreviation"));
        addAbbreviationButton.setGraphic(IconTheme.JabRefIcons.ADD_NOBOX.getGraphicNode());
        addAbbreviationButton.disableProperty().bind(viewModel.isEditableAndRemovableProperty().not());
        addAbbreviationButton.setOnAction(_ -> addAbbreviation());

        HBox row = new HBox(10.0, addAbbreviationButton);
        row.setAlignment(Pos.BASELINE_RIGHT);
        return row;
    }

    private void setAnimations() {
        ObjectProperty<Color> flashingColor = new SimpleObjectProperty<>(Color.TRANSPARENT);
        StringProperty flashingColorStringProperty = ColorUtil.createFlashingColorStringProperty(flashingColor);

        searchBox.styleProperty().bind(
                new SimpleStringProperty("-fx-control-inner-background: ").concat(flashingColorStringProperty).concat(";")
        );
        invalidateSearch = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(flashingColor, Color.TRANSPARENT, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(0.25), new KeyValue(flashingColor, Color.RED, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(0.25), new KeyValue(searchBox.textProperty(), "", Interpolator.DISCRETE)),
                new KeyFrame(Duration.seconds(0.25), (ActionEvent _) -> addAbbreviationActions()),
                new KeyFrame(Duration.seconds(0.5), new KeyValue(flashingColor, Color.TRANSPARENT, Interpolator.LINEAR))
        );
    }

    private void addAbbreviation() {
        if (!searchBox.getText().isEmpty()) {
            invalidateSearch.play();
        } else {
            addAbbreviationActions();
        }
    }

    private void addAbbreviationActions() {
        viewModel.addAbbreviation();
        selectNewAbbreviation();
        editAbbreviation();
    }

    private void editAbbreviation() {
        journalAbbreviationsTable.edit(
                journalAbbreviationsTable.getSelectionModel().getSelectedIndex(),
                journalTableNameColumn);
    }

    private void selectNewAbbreviation() {
        int lastRow = viewModel.abbreviationsCountProperty().get() - 1;
        journalAbbreviationsTable.scrollTo(lastRow);
        journalAbbreviationsTable.getSelectionModel().select(lastRow);
        journalAbbreviationsTable.getFocusModel().focus(lastRow, journalTableNameColumn);
    }
}
