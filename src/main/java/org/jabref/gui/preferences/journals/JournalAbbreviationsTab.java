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
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

/**
 * This class controls the user interface of the journal abbreviations dialog. The UI elements and their layout are
 * defined in the FXML file.
 */
public class JournalAbbreviationsTab extends AbstractPreferenceTabView<JournalAbbreviationsTabViewModel> implements PreferencesTab {

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
        
        viewModel.journalFilesProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            for (AbbreviationsFileViewModel fileViewModel : newValue) {
                fileViewModel.enabledProperty().addListener((obs, oldVal, newVal) -> {
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
                String prefix = item.isEnabled() ? "✓ " : "○ ";
                setText(prefix + item.toString());
                
                item.enabledProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        setText((newVal ? "✓ " : "○ ") + item.toString());
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
    
    @FXML
    private void toggleEnableList() {
        AbbreviationsFileViewModel selected = journalFilesBox.getValue();
        if (selected != null) {
            boolean newEnabledState = !selected.isEnabled();
            selected.setEnabled(newEnabledState);
            
            refreshComboBoxDisplay();
            
            JournalAbbreviationPreferences abbreviationPreferences = preferences.getJournalAbbreviationPreferences();
                
            if (selected.isBuiltInListProperty().get()) {
                abbreviationPreferences.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, newEnabledState);
            } else if (selected.getAbsolutePath().isPresent()) {
                String fileName = selected.getAbsolutePath().get().getFileName().toString();
                abbreviationPreferences.setSourceEnabled(fileName, newEnabledState);
            }
            
            JournalAbbreviationRepository newRepository = 
                JournalAbbreviationLoader.loadRepository(abbreviationPreferences);
                
            Injector.setModelOrService(JournalAbbreviationRepository.class, newRepository);
            
            abbreviationRepository = newRepository;
            
            viewModel.markAsDirty();
        }
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
