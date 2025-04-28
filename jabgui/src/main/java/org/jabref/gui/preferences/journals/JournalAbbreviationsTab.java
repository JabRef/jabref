package org.jabref.gui.preferences.journals;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ColorUtil;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

/**
 * This class controls the user interface of the journal abbreviations dialog. The UI elements and their layout are
 * defined in the FXML file.
 */
public class JournalAbbreviationsTab extends AbstractPreferenceTabView<JournalAbbreviationsTabViewModel> implements PreferencesTab {

    private static final String ENABLED_SYMBOL = "✓ ";
    private static final String DISABLED_SYMBOL = "○ ";

    @FXML private Label loadingLabel;
    @FXML private ProgressIndicator progressIndicator;

    @FXML private TableView<AbbreviationViewModel> journalAbbreviationsTable;
    @FXML private TableColumn<AbbreviationViewModel, String> journalTableNameColumn;
    @FXML private TableColumn<AbbreviationViewModel, String> journalTableAbbreviationColumn;
    @FXML private TableColumn<AbbreviationViewModel, String> journalTableShortestUniqueAbbreviationColumn;
    @FXML private TableColumn<AbbreviationViewModel, String> actionsColumn;

    private FilteredList<AbbreviationViewModel> filteredAbbreviations;
    @FXML private ComboBox<AbbreviationsFileViewModel> journalFilesBox;

    @FXML private Button addAbbreviationButton;
    @FXML private Button removeAbbreviationListButton;

    @FXML private CustomTextField searchBox;
    @FXML private CheckBox useFJournal;

    @Inject private TaskExecutor taskExecutor;
    @Inject private JournalAbbreviationRepository abbreviationRepository;

    private Timeline invalidateSearch;

    public JournalAbbreviationsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new JournalAbbreviationsTabViewModel(
                preferences.getJournalAbbreviationPreferences(),
                dialogService,
                taskExecutor,
                abbreviationRepository);

        filteredAbbreviations = new FilteredList<>(viewModel.abbreviationsProperty());

        setUpTable();
        setUpToggleButton();
        setBindings();
        setAnimations();

        searchBox.setPromptText(Localization.lang("Search..."));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
    }

    private void setUpTable() {
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        journalTableAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        journalTableAbbreviationColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        journalTableShortestUniqueAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().shortestUniqueAbbreviationProperty());
        journalTableShortestUniqueAbbreviationColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<AbbreviationViewModel, String>()
                .withGraphic(name -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove journal '%0'", name))
                .withDisableExpression(item -> viewModel.isEditableAndRemovableProperty().not())
                .withVisibleExpression(item -> viewModel.isEditableAndRemovableProperty())
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.removeAbbreviation(journalAbbreviationsTable.getFocusModel().getFocusedItem()))
                .install(actionsColumn);
    }

    /**
     * Sets up the toggle button that allows enabling/disabling journal abbreviation lists.
     * This method creates a button with appropriate styling and tooltip, then adds it
     * to the UI next to the journal files dropdown. When clicked, the button toggles 
     * the enabled state of the currently selected abbreviation list.
     */
    private void setUpToggleButton() {
        Button toggleButton = new Button(Localization.lang("Toggle"));
        toggleButton.setOnAction(e -> toggleEnableList());
        toggleButton.setTooltip(new Tooltip(Localization.lang("Toggle selected list on/off")));
        toggleButton.getStyleClass().add("icon-button");
        
        for (Node node : getChildren()) {
            if (node instanceof HBox hbox) {
                boolean containsComboBox = false;
                for (Node child : hbox.getChildren()) {
                    if (child == journalFilesBox) {
                        containsComboBox = true;
                        break;
                    }
                }
                
                if (containsComboBox) {
                    int comboBoxIndex = hbox.getChildren().indexOf(journalFilesBox);
                    hbox.getChildren().add(comboBoxIndex + 1, toggleButton);
                    break;
                }
            }
        }
    }

    private void setBindings() {
        journalAbbreviationsTable.setItems(filteredAbbreviations);

        EasyBind.subscribe(journalAbbreviationsTable.getSelectionModel().selectedItemProperty(), newValue ->
                viewModel.currentAbbreviationProperty().set(newValue));
        EasyBind.subscribe(viewModel.currentAbbreviationProperty(), newValue ->
                journalAbbreviationsTable.getSelectionModel().select(newValue));

        journalTableNameColumn.editableProperty().bind(viewModel.isAbbreviationEditableAndRemovable());
        journalTableAbbreviationColumn.editableProperty().bind(viewModel.isAbbreviationEditableAndRemovable());
        journalTableShortestUniqueAbbreviationColumn.editableProperty().bind(viewModel.isAbbreviationEditableAndRemovable());

        removeAbbreviationListButton.disableProperty().bind(viewModel.isFileRemovableProperty().not());
        journalFilesBox.itemsProperty().bindBidirectional(viewModel.journalFilesProperty());
        journalFilesBox.valueProperty().bindBidirectional(viewModel.currentFileProperty());
        
        journalFilesBox.setCellFactory(listView -> new JournalFileListCell());
        journalFilesBox.setButtonCell(new JournalFileListCell());
        
        viewModel.journalFilesProperty().addListener((_, _, newValue) -> {
            if (newValue == null) {
                return;
            }
            for (AbbreviationsFileViewModel fileViewModel : newValue) {
                fileViewModel.enabledProperty().addListener((_, _, _) -> {
                    refreshComboBoxDisplay();
                });
            }
        });

        addAbbreviationButton.disableProperty().bind(viewModel.isEditableAndRemovableProperty().not());

        loadingLabel.visibleProperty().bind(viewModel.isLoadingProperty());
        progressIndicator.visibleProperty().bind(viewModel.isLoadingProperty());

        searchBox.textProperty().addListener((observable, previousText, searchTerm) ->
                filteredAbbreviations.setPredicate(abbreviation -> searchTerm.isEmpty() || abbreviation.containsCaseIndependent(searchTerm)));

        useFJournal.selectedProperty().bindBidirectional(viewModel.useFJournalProperty());
    }

    /**
     * Custom ListCell to display the journal file items with checkboxes.
     * This simply shows the checkbox status without trying to handle
     * direct checkbox interactions, to avoid conflicts with ComboBox selection.
     */
    private static class JournalFileListCell extends ListCell<AbbreviationsFileViewModel> {
        @Override
        protected void updateItem(AbbreviationsFileViewModel item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String prefix = item.isEnabled() ? ENABLED_SYMBOL : DISABLED_SYMBOL;
                setText(prefix + item.toString());
                
                item.enabledProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        setText((newVal ? ENABLED_SYMBOL : DISABLED_SYMBOL) + item.toString());
                    }
                });
            }
        }
    }
    
    /**
     * Force the ComboBox to refresh its display
     */
    private void refreshComboBoxDisplay() {
        Platform.runLater(() -> {
            AbbreviationsFileViewModel currentSelection = journalFilesBox.getValue();
            
            journalFilesBox.setButtonCell(new JournalFileListCell());
            
            journalFilesBox.setValue(null);
            journalFilesBox.setValue(currentSelection);
            
            journalFilesBox.setCellFactory(listView -> new JournalFileListCell());
            
            journalFilesBox.requestLayout();
        });
    }

    @FXML
    private void addList() {
        viewModel.addNewFile();
    }

    @FXML
    private void openList() {
        viewModel.openFile();
    }

    @FXML
    private void removeList() {
        viewModel.removeCurrentFile();
    }

    @FXML
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

    @FXML
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

    @Override
    public String getTabName() {
        return Localization.lang("Journal abbreviations");
    }
    
    /**
     * Toggles the enabled state of the currently selected journal abbreviation list.
     * This method performs several important operations:
     * <ul>
     *   <li>Toggles the enabled state of the selected list in the UI</li>
     *   <li>Refreshes the ComboBox display to show the updated state</li>
     *   <li>Updates the JournalAbbreviationPreferences to persist this change</li>
     *   <li>Reloads the entire JournalAbbreviationRepository with the new settings</li>
     *   <li>Updates the dependency injection container with the new repository</li>
     *   <li>Marks the view model as dirty to ensure changes are saved</li>
     * </ul>
     * This is called when the user clicks the toggle button next to the journal files dropdown.
     */
    @FXML
    private void toggleEnableList() {
        AbbreviationsFileViewModel selected = journalFilesBox.getValue();
        if (selected == null) {
            return;
        }
        
        boolean newEnabledState = !selected.isEnabled();
        selected.setEnabled(newEnabledState);
        
        refreshComboBoxDisplay();
        
        viewModel.markAsDirty();
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
                new KeyFrame(Duration.seconds(0.25), (ActionEvent event) -> addAbbreviationActions()),
                new KeyFrame(Duration.seconds(0.5), new KeyValue(flashingColor, Color.TRANSPARENT, Interpolator.LINEAR))
        );
    }
}
