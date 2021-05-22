package org.jabref.gui.slr;

import java.nio.file.Path;
import java.util.Objects;
import java.util.StringJoiner;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.study.Study;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * This class controls the user interface of the study definition management dialog. The UI elements and their layout are
 * defined in the FXML file.
 */
public class ManageStudyDefinitionView extends BaseDialog<SlrStudyAndDirectory> {
    Path workingDirectory;

    @Inject DialogService dialogService;
    @Inject PreferencesService prefs;

    private ManageStudyDefinitionViewModel viewModel;
    private final Study study;

    @FXML private TextField studyTitle;
    @FXML private TextField addAuthor;
    @FXML private TextField addResearchQuestion;
    @FXML private TextField addQuery;
    @FXML private ComboBox<StudyDatabaseItem> databaseSelectorComboBox;
    @FXML private TextField studyDirectory;

    private Button saveButton;

    @FXML private Button addAuthorButton;
    @FXML private Button addResearchQuestionButton;
    @FXML private Button addQueryButton;
    @FXML private Button addDatabaseButton;
    @FXML private Button selectStudyDirectory;
    @FXML private ButtonType saveButtonType;
    @FXML private Label helpIcon;

    @FXML private TableView<String> authorTableView;
    @FXML private TableColumn<String, String> authorsColumn;
    @FXML private TableColumn<String, String> authorsActionColumn;

    @FXML private TableView<String> questionTableView;
    @FXML private TableColumn<String, String> questionsColumn;
    @FXML private TableColumn<String, String> questionsActionColumn;

    @FXML private TableView<String> queryTableView;
    @FXML private TableColumn<String, String> queriesColumn;
    @FXML private TableColumn<String, String> queriesActionColumn;

    @FXML private TableView<StudyDatabaseItem> databaseTableView;
    @FXML private TableColumn<StudyDatabaseItem, String> databaseTableEntry;
    @FXML private TableColumn<StudyDatabaseItem, Boolean> databaseEnabledTableEntry;
    @FXML private TableColumn<StudyDatabaseItem, String> removeDatabaseTableEntry;

    /**
     * This can be used to either create new study objects or edit existing ones.
     *
     * @param study          null if a new study is created. Otherwise the study object to edit.
     * @param studyDirectory the directory where the study to edit is located (null if a new study is created)
     */
    public ManageStudyDefinitionView(Study study, Path studyDirectory, Path workingDirectory) {
        // If an existing study is edited, open the directory dialog at the directory the study is stored
        this.workingDirectory = Objects.isNull(studyDirectory) ? workingDirectory : studyDirectory;
        this.setTitle(Objects.isNull(studyDirectory) ? Localization.lang("Define study parameters") : Localization.lang("Manage study definition"));
        this.study = study;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setupSaveButton();
        setupTable();
    }

    private void setupSaveButton() {
        Button saveButton = ((Button) this.getDialogPane().lookupButton(saveButtonType));

        saveButton.disableProperty().bind(Bindings.or(Bindings.or(
                Bindings.or(
                        Bindings.or(Bindings.isEmpty(viewModel.getQueries()), Bindings.isEmpty(viewModel.getDatabases())),
                        Bindings.isEmpty(viewModel.getAuthors())),
                viewModel.getTitle().isEmpty()), viewModel.getDirectory().isEmpty()));

        setResultConverter(button -> {
            if (button == saveButtonType) {
                return viewModel.saveStudy();
            }
            // Cancel button will return null
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ManageStudyDefinitionViewModel(study, workingDirectory, prefs.getImportFormatPreferences());
        setHelpTooltip();
        setKeyPressListenersForInputFields();
        setDataForListViews();
        setCellFactories();
        configureLayout();
        registerBindings();
    }

    private void setHelpTooltip() {
        // TODO: Keep until PR #7279 is merged
        helpIcon.setTooltip(new Tooltip(new StringJoiner("\n")
                .add(Localization.lang("Query terms are separated by spaces."))
                .add(Localization.lang("All query terms are joined using the logical AND, and OR operators") + ".")
                .add(Localization.lang("If the sequence of terms is relevant wrap them in double quotes") + "(\").")
                .add(Localization.lang("An example:") + " rain AND (clouds OR drops) AND \"precipitation distribution\"")
                .toString()));
    }

    private void setKeyPressListenersForInputFields() {
        addAuthor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addAuthor();
            }
        });
        addResearchQuestion.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addResearchQuestion();
            }
        });
        addQuery.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addQuery();
            }
        });
        databaseSelectorComboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addDatabase();
            }
        });
    }

    private void setDataForListViews() {
        authorTableView.setItems(viewModel.getAuthors());
        questionTableView.setItems(viewModel.getResearchQuestions());
        queryTableView.setItems(viewModel.getQueries());
        databaseTableView.setItems(viewModel.getDatabases());
        databaseSelectorComboBox.setItems(viewModel.getNonSelectedDatabases());
    }

    private void setupTable() {
        authorsColumn.setReorderable(false);
        authorsActionColumn.setReorderable(false);
        authorsActionColumn.setResizable(false);
        questionsColumn.setReorderable(false);
        questionsActionColumn.setReorderable(false);
        questionsActionColumn.setResizable(false);
        queriesColumn.setReorderable(false);
        queriesActionColumn.setReorderable(false);
        queriesActionColumn.setResizable(false);
        databaseTableEntry.setReorderable(false);
        databaseEnabledTableEntry.setResizable(false);
        databaseEnabledTableEntry.setReorderable(false);
        removeDatabaseTableEntry.setResizable(false);
        removeDatabaseTableEntry.setReorderable(false);
    }

    /**
     * Configures which cells are used for the different List-/TableViews
     */
    private void setCellFactories() {
        authorsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        authorsColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        authorsActionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        new ValueTableCellFactory<String, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove"))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.deleteAuthor(item))
                .install(authorsActionColumn);

        questionsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        questionsColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        questionsActionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        new ValueTableCellFactory<String, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove"))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.deleteQuestion(item))
                .install(questionsActionColumn);

        queriesColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        queriesColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        queriesActionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        new ValueTableCellFactory<String, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove"))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.deleteQuery(item))
                .install(queriesActionColumn);

        new ViewModelListCellFactory<StudyDatabaseItem>().withText(StudyDatabaseItem::getName)
                                      .install(databaseSelectorComboBox);

        // databaseTableEntry.setCellFactory(TextFieldTableCell.forTableColumn());
        databaseTableEntry.setCellValueFactory(param -> param.getValue().nameProperty());

        databaseEnabledTableEntry.setCellValueFactory(param -> param.getValue().enabledProperty());
        databaseEnabledTableEntry.setCellFactory(CheckBoxTableCell.forTableColumn(databaseEnabledTableEntry));

        removeDatabaseTableEntry.setCellValueFactory(param -> param.getValue().nameProperty());
        new ValueTableCellFactory<org.jabref.gui.slr.StudyDatabaseItem, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove"))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.removeDatabase(item))
                .install(removeDatabaseTableEntry);
    }

    private void configureLayout() {
        databaseTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void registerBindings() {
        // Listen whether any databases are removed from selection -> Add back to the database selector
        studyTitle.textProperty().bindBidirectional(viewModel.titleProperty());
        studyDirectory.textProperty().bindBidirectional(viewModel.getDirectory());
    }

    @FXML
    private void addAuthor() {
        viewModel.addAuthor(addAuthor.getText());
        addAuthor.setText("");
    }

    @FXML
    private void addResearchQuestion() {
        viewModel.addResearchQuestion(addResearchQuestion.getText());
        addResearchQuestion.setText("");
    }

    @FXML
    private void addQuery() {
        viewModel.addQuery(addQuery.getText());
        addQuery.setText("");
    }

    /**
     * Add selected entry from combobox, push onto database pop from nonselecteddatabase (combobox)
     */
    @FXML
    private void addDatabase() {
        viewModel.addDatabase(databaseSelectorComboBox.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void selectStudyDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        viewModel.setStudyDirectory(dialogService.showDirectorySelectionDialog(directoryDialogConfiguration));
    }
}
