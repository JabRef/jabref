package org.jabref.gui.slr;

import java.nio.file.Path;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.study.Study;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class controls the user interface of the study definition management dialog. The UI elements and their layout
 * are defined in the FXML file.
 */
public class ManageStudyDefinitionView extends BaseDialog<SlrStudyAndDirectory> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageStudyDefinitionView.class);

    @FXML private TextField studyTitle;
    @FXML private TextField addAuthor;
    @FXML private TextField addResearchQuestion;
    @FXML private TextField addQuery;
    @FXML private TextField studyDirectory;
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

    @FXML private TableView<StudyDatabaseItem> databaseTable;
    @FXML private TableColumn<StudyDatabaseItem, Boolean> databaseEnabledColumn;
    @FXML private TableColumn<StudyDatabaseItem, String> databaseColumn;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService prefs;
    @Inject private ThemeManager themeManager;

    private ManageStudyDefinitionViewModel viewModel;

    // not present if new study is created;
    // present if existing study is edited
    private final Optional<Study> study;

    // Either the proposed directory (on new study creation)
    // or the "real" directory of the study
    private final Path pathToStudyDataDirectory;

    /**
     * This is used to create a new study
     *
     * @param pathToStudyDataDirectory This directory is proposed in the file chooser
     */
    public ManageStudyDefinitionView(Path pathToStudyDataDirectory) {
        this.pathToStudyDataDirectory = pathToStudyDataDirectory;
        this.setTitle("Define study parameters");
        this.study = Optional.empty();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setupSaveButton();

        themeManager.updateFontStyle(getDialogPane().getScene());
    }

    /**
     * This is used to edit an existing study.
     *
     * @param study          the study to edit
     * @param studyDirectory the directory of the study
     */
    public ManageStudyDefinitionView(Study study, Path studyDirectory) {
        this.pathToStudyDataDirectory = studyDirectory;
        this.setTitle(Localization.lang("Manage study definition"));
        this.study = Optional.of(study);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setupSaveButton();

        themeManager.updateFontStyle(getDialogPane().getScene());
    }

    private void setupSaveButton() {
        Button saveButton = ((Button) this.getDialogPane().lookupButton(saveButtonType));

        saveButton.disableProperty().bind(Bindings.or(Bindings.or(Bindings.or(Bindings.or(
                                Bindings.isEmpty(viewModel.getQueries()),
                                Bindings.isEmpty(viewModel.getDatabases())),
                                Bindings.isEmpty(viewModel.getAuthors())),
                                viewModel.getTitle().isEmpty()),
                                viewModel.getDirectory().isEmpty()));

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
        if (study.isEmpty()) {
            viewModel = new ManageStudyDefinitionViewModel(
                    prefs.getImportFormatPreferences(),
                    prefs.getImporterPreferences(),
                    dialogService);
        } else {
            viewModel = new ManageStudyDefinitionViewModel(
                    study.get(),
                    pathToStudyDataDirectory,
                    prefs.getImportFormatPreferences(),
                    prefs.getImporterPreferences(),
                    dialogService);

            // The directory of the study cannot be changed
            studyDirectory.setEditable(false);
            selectStudyDirectory.setDisable(true);
        }

        // Listen whether any databases are removed from selection -> Add back to the database selector
        studyTitle.textProperty().bindBidirectional(viewModel.titleProperty());
        studyDirectory.textProperty().bindBidirectional(viewModel.getDirectory());

        initAuthorTab();
        initQuestionsTab();
        initQueriesTab();
        initDatabasesTab();
    }

    private void initAuthorTab() {
        setupCommonPropertiesForTables(addAuthor, this::addAuthor, authorsColumn, authorsActionColumn);
        setupCellFactories(authorsColumn, authorsActionColumn, viewModel::deleteAuthor);
        authorTableView.setItems(viewModel.getAuthors());
    }

    private void initQuestionsTab() {
        setupCommonPropertiesForTables(addResearchQuestion, this::addResearchQuestion, questionsColumn, questionsActionColumn);
        setupCellFactories(questionsColumn, questionsActionColumn, viewModel::deleteQuestion);
        questionTableView.setItems(viewModel.getResearchQuestions());
    }

    private void initQueriesTab() {
        setupCommonPropertiesForTables(addQuery, this::addQuery, queriesColumn, queriesActionColumn);
        setupCellFactories(queriesColumn, queriesActionColumn, viewModel::deleteQuery);
        queryTableView.setItems(viewModel.getQueries());

        // TODO: Keep until PR #7279 is merged
        helpIcon.setTooltip(new Tooltip(new StringJoiner("\n")
                .add(Localization.lang("Query terms are separated by spaces."))
                .add(Localization.lang("All query terms are joined using the logical AND, and OR operators") + ".")
                .add(Localization.lang("If the sequence of terms is relevant wrap them in double quotes") + "(\").")
                .add(Localization.lang("An example:") + " rain AND (clouds OR drops) AND \"precipitation distribution\"")
                .toString()));
    }

    private void initDatabasesTab() {
        new ViewModelTableRowFactory<StudyDatabaseItem>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        entry.setEnabled(!entry.isEnabled());
                    }
                })
                .install(databaseTable);

        databaseColumn.setReorderable(false);
        databaseColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        databaseEnabledColumn.setResizable(false);
        databaseEnabledColumn.setReorderable(false);
        databaseEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(databaseEnabledColumn));
        databaseEnabledColumn.setCellValueFactory(param -> param.getValue().enabledProperty());

        databaseColumn.setEditable(false);
        databaseColumn.setCellValueFactory(param -> param.getValue().nameProperty());

        databaseTable.setItems(viewModel.getDatabases());
    }

    private void setupCommonPropertiesForTables(Node addControl,
                                                Runnable addAction,
                                                TableColumn<?, String> contentColumn,
                                                TableColumn<?, String> actionColumn) {
        addControl.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addAction.run();
            }
        });

        contentColumn.setReorderable(false);
        contentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        actionColumn.setReorderable(false);
        actionColumn.setResizable(false);
    }

    private void setupCellFactories(TableColumn<String, String> contentColumn,
                                    TableColumn<String, String> actionColumn,
                                    Consumer<String> removeAction) {
        contentColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        actionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
        new ValueTableCellFactory<String, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove"))
                .withOnMouseClickedEvent(item -> evt ->
                        removeAction.accept(item))
                .install(actionColumn);
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

    @FXML
    public void selectStudyDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(pathToStudyDataDirectory)
                .build();

        viewModel.setStudyDirectory(dialogService.showDirectorySelectionDialog(directoryDialogConfiguration));
    }
}
