package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.CheckTreeView;

/*
 * TODO: Adapt to MVVM architecture
 */
public class ParseTexDialog extends BaseDialog<Void> {

    private final BibDatabaseContext databaseContext;

    @FXML private TextField texDirectoryField;
    @FXML private Button browseButton;
    @FXML private Button searchButton;
    @FXML private CheckTreeView<FileNode> filesTree;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ButtonType parseButtonType;

    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;

    private Path initialPath;
    private Button parseButton;
    private BackgroundTask searchFilesTask;

    public ParseTexDialog(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        this.setTitle(Localization.lang("LaTeX integration tools"));
        ViewLoader.view(this).load().setAsDialogPane(this);

        ControlHelper.setAction(parseButtonType, getDialogPane(), e -> parse());
        this.parseButton = (Button) getDialogPane().lookupButton(parseButtonType);
        this.parseButton.setDisable(true);
    }

    @FXML
    private void initialize() {
        initialPath = databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                     .orElse(preferencesService.getWorkingDir());

        texDirectoryField.setText(initialPath.toAbsolutePath().toString());

        new ViewModelTreeCellFactory<FileNode>()
                .withText(node -> node.toString())
                .install(filesTree);

        selectAllButton.setDisable(true);
        unselectAllButton.setDisable(true);
    }

    @FXML
    private void browseButtonClicked() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(initialPath).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory -> {
            texDirectoryField.setText(selectedDirectory.toAbsolutePath().toString());
            preferencesService.setWorkingDir(selectedDirectory.toAbsolutePath());
        });
    }

    @FXML
    private void searchButtonClicked() {
        searchFilesTask = new TexFileCrawler(getSearchDirectory())
                .onRunning(() -> {
                    filesTree.setRoot(null);
                    browseButton.setDisable(true);
                    searchButton.setDisable(true);
                    selectAllButton.setDisable(true);
                    unselectAllButton.setDisable(true);
                    parseButton.setDisable(true);
                })
                .onFinished(() -> {
                    browseButton.setDisable(false);
                    searchButton.setDisable(false);
                })
                .onSuccess(root -> {
                    filesTree.setRoot(root);
                    root.setSelected(true);
                    root.setExpanded(true);
                    selectAllButton.setDisable(false);
                    unselectAllButton.setDisable(false);
                    parseButton.setDisable(false);
                });

        searchFilesTask.executeWith(taskExecutor);
    }

    private Path getSearchDirectory() {
        Path directory = Paths.get(texDirectoryField.getText());

        if (Files.notExists(directory)) {
            directory =  databaseContext.getFirstExistingFileDir(Globals.prefs.getFilePreferences())
                                        .orElse(preferencesService.getWorkingDir());

            texDirectoryField.setText(directory.toAbsolutePath().toString());
        }

        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();

            texDirectoryField.setText(directory.toAbsolutePath().toString());
        }

        return directory;
    }

    @FXML
    private void selectAll() {
        CheckBoxTreeItem<FileNode> root = (CheckBoxTreeItem<FileNode>) filesTree.getRoot();

        root.setSelected(true);
        root.setSelected(false);
        root.setSelected(true);
    }

    @FXML
    private void unselectAll() {
        CheckBoxTreeItem<FileNode> root = (CheckBoxTreeItem<FileNode>) filesTree.getRoot();

        root.setSelected(false);
        root.setSelected(true);
        root.setSelected(false);
    }

    @FXML
    private void parse() {
        if (filesTree.getRoot() == null) {
            return;
        }

        List<Path> fileList = filesTree.getCheckModel()
                                       .getCheckedItems()
                                       .stream()
                                       .filter(node -> node.isLeaf())
                                       .map(node -> node.getValue().getPath())
                                       .filter(path -> path != null && Files.isRegularFile(path))
                                       .collect(Collectors.toList());

        if (fileList.isEmpty()) {
            return;
        }

        TexParserResult result = new DefaultTexParser().parse(fileList);
        ParseTexResultView dialog = new ParseTexResultView(result);

        dialog.showAndWait();
    }
}
