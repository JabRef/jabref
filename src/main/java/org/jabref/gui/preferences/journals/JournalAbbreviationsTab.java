package org.jabref.gui.preferences.journals;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

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
    @FXML private ComboBox<AbbreviationsFileViewModel> journalFilesBox;
    @FXML private Button addAbbreviationButton;
    @FXML private Button removeAbbreviationButton;
    @FXML private Button openAbbreviationListButton;
    @FXML private Button addAbbreviationListButton;
    @FXML private Button removeAbbreviationListButton;

    @Inject private TaskExecutor taskExecutor;
    @Inject private JournalAbbreviationRepository abbreviationRepository;

    public JournalAbbreviationsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new JournalAbbreviationsTabViewModel(preferencesService, dialogService, taskExecutor, abbreviationRepository);

        setButtonStyles();
        setUpTable();
        setBindings();
    }

    private void setButtonStyles() {
        addAbbreviationListButton.setGraphic(IconTheme.JabRefIcons.ADD_ABBREVIATION_LIST.getGraphicNode());
        openAbbreviationListButton.setGraphic(IconTheme.JabRefIcons.OPEN_ABBREVIATION_LIST.getGraphicNode());
        removeAbbreviationListButton.setGraphic(IconTheme.JabRefIcons.REMOVE_ABBREVIATION_LIST.getGraphicNode());
        addAbbreviationButton.setGraphic(IconTheme.JabRefIcons.ADD_ABBREVIATION.getGraphicNode());
        removeAbbreviationButton.setGraphic(IconTheme.JabRefIcons.REMOVE_ABBREVIATION.getGraphicNode());
    }

    private void setUpTable() {
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        journalTableAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        journalTableAbbreviationColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        journalTableShortestUniqueAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().shortestUniqueAbbreviationProperty());
        journalTableShortestUniqueAbbreviationColumn.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    private void setBindings() {
        journalAbbreviationsTable.itemsProperty().bindBidirectional(viewModel.abbreviationsProperty());

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

        addAbbreviationButton.disableProperty().bind(viewModel.isEditableAndRemovableProperty().not());
        removeAbbreviationButton.disableProperty().bind(viewModel.isAbbreviationEditableAndRemovable().not());

        loadingLabel.visibleProperty().bind(viewModel.isLoadingProperty());
        progressIndicator.visibleProperty().bind(viewModel.isLoadingProperty());
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

    @FXML
    private void removeAbbreviation() {
        viewModel.deleteAbbreviation();
    }

    private void selectNewAbbreviation() {
        int lastRow = viewModel.abbreviationsCountProperty().get() - 1;
        journalAbbreviationsTable.scrollTo(lastRow);
        journalAbbreviationsTable.getSelectionModel().select(lastRow);
        journalAbbreviationsTable.getFocusModel().focus(lastRow);
    }

    @Override
    public String getTabName() {
        return Localization.lang("Journal abbreviations");
    }
}
