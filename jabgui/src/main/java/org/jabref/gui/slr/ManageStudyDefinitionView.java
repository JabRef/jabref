package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.study.Study;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/**
 * This class controls the user interface of the study definition management dialog. The UI elements and their layout
 * are defined in the FXML file.
 */
public class ManageStudyDefinitionView extends BaseDialog<SlrStudyAndDirectory> {
    @FXML private TextField studyTitle;
    @FXML private TextField addAuthor;
    @FXML private TextField addResearchQuestion;
    @FXML private TextField addQuery;
    @FXML private TextField studyDirectory;
    @FXML private Button selectStudyDirectory;

    @FXML private ButtonType saveSurveyButtonType;
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

    @FXML private TableView<StudyCatalogItem> catalogTable;
    @FXML private TableColumn<StudyCatalogItem, Boolean> catalogEnabledColumn;
    @FXML private TableColumn<StudyCatalogItem, String> catalogColumn;

    @FXML private Label directoryWarning;

    @Inject private DialogService dialogService;
    @Inject private GuiPreferences preferences;

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
        this.setTitle(Localization.lang("Define study parameters"));
        this.study = Optional.empty();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setupSaveSurveyButton(false);
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

        setupSaveSurveyButton(true);
    }

    private void setupSaveSurveyButton(boolean isEdit) {
        Button saveSurveyButton = (Button) this.getDialogPane().lookupButton(saveSurveyButtonType);

        if (!isEdit) {
            saveSurveyButton.setText(Localization.lang("Start survey"));
        }

        saveSurveyButton.disableProperty().bind(Bindings.or(Bindings.or(Bindings.or(Bindings.or(Bindings.or(
                                                Bindings.isEmpty(viewModel.getQueries()),
                                                Bindings.isEmpty(viewModel.getCatalogs())),
                                        Bindings.isEmpty(viewModel.getAuthors())),
                                viewModel.getTitle().isEmpty()),
                        viewModel.getDirectory().isEmpty()),
                directoryWarning.visibleProperty()));

        setResultConverter(button -> {
            if (button == saveSurveyButtonType) {
                viewModel.updateSelectedCatalogs();
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
                    preferences.getImportFormatPreferences(),
                    preferences.getImporterPreferences(),
                    preferences.getWorkspacePreferences(),
                    dialogService);
        } else {
            viewModel = new ManageStudyDefinitionViewModel(
                    study.get(),
                    pathToStudyDataDirectory,
                    preferences.getImportFormatPreferences(),
                    preferences.getImporterPreferences(),
                    preferences.getWorkspacePreferences(),
                    dialogService);

            // The directory of the study cannot be changed
            studyDirectory.setEditable(false);
            selectStudyDirectory.setDisable(true);
        }

        // Listen whether any catalogs are removed from selection -> Add back to the catalog selector
        studyTitle.textProperty().bindBidirectional(viewModel.titleProperty());
        studyDirectory.textProperty().bindBidirectional(viewModel.getDirectory());

        initAuthorTab();
        initQuestionsTab();
        initQueriesTab();
        initCatalogsTab();
    }

    private void updateDirectoryWarning(Path directory) {
        if (!Files.isDirectory(directory)) {
            directoryWarning.setText(Localization.lang("Warning: The selected directory is not a valid directory."));
            directoryWarning.setVisible(true);
        } else {
            try (Stream<Path> entries = Files.list(directory)) {
                if (entries.findAny().isPresent()) {
                    directoryWarning.setText(Localization.lang("Warning: The selected directory is not empty."));
                    directoryWarning.setVisible(true);
                } else {
                    directoryWarning.setVisible(false);
                }
            } catch (IOException e) {
                directoryWarning.setText(Localization.lang("Warning: Failed to check if the directory is empty."));
                directoryWarning.setVisible(true);
            }
        }
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

    private void initCatalogsTab() {
        new ViewModelTableRowFactory<StudyCatalogItem>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        entry.setEnabled(!entry.isEnabled());
                    }
                })
                .install(catalogTable);

        if (study.isEmpty()) {
            viewModel.initializeSelectedCatalogs();
        }

        catalogColumn.setReorderable(false);
        catalogColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        catalogEnabledColumn.setResizable(false);
        catalogEnabledColumn.setReorderable(false);
        catalogEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(catalogEnabledColumn));
        catalogEnabledColumn.setCellValueFactory(param -> param.getValue().enabledProperty());

        catalogColumn.setEditable(false);
        catalogColumn.setCellValueFactory(param -> param.getValue().nameProperty());

        catalogTable.setItems(viewModel.getCatalogs());
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

        Optional<Path> selectedDirectoryOptional = dialogService.showDirectorySelectionDialog(directoryDialogConfiguration);
        selectedDirectoryOptional.ifPresent(selectedDirectory -> {
            viewModel.setStudyDirectory(Optional.of(selectedDirectory));
            updateDirectoryWarning(selectedDirectory);
        });
    }
}
