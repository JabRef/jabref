package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ParseTexDialog extends BaseDialog<Void> {

    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    @FXML private TextField texDirectoryField;
    @FXML private Button browseButton;
    @FXML private Button searchButton;
    @FXML private TreeView<FileNode> filesTree;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ButtonType parseButtonType;
    private Button parseButton;
    private BackgroundTask searchFilesTask;

    public ParseTexDialog(DialogService dialogService, BibDatabaseContext databaseContext) {
        super();
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.setTitle(Localization.lang("LaTeX integration tools"));
        ViewLoader.view(this).load().setAsDialogPane(this);

        ControlHelper.setAction(parseButtonType, getDialogPane(), e -> parse());
        this.parseButton = (Button) getDialogPane().lookupButton(parseButtonType);
        this.parseButton.setDisable(true);
    }

    @FXML
    private void initialize() {
        Path initialPath = databaseContext.getFirstExistingFileDir(Globals.prefs.getFilePreferences())
                                          .orElse(Globals.prefs.getWorkingDir());

        texDirectoryField.setText(initialPath.toAbsolutePath().toString());

        new ViewModelTreeCellFactory<FileNode>().withText(node -> {
            Path path = node.getPath();
            int fileCount = node.getFileCount();

            if (Files.isRegularFile(path)) {
                return path.getFileName().toString();
            } else {
                return String.format("%s (%s %s)", path.getFileName(), fileCount,
                        fileCount == 1 ? Localization.lang("file") : Localization.lang("files"));
            }
        }).install(filesTree);

        selectAllButton.setDisable(true);
        unselectAllButton.setDisable(true);
    }

    @FXML
    private void browseButtonClicked() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory -> {
            texDirectoryField.setText(selectedDirectory.toAbsolutePath().toString());
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, selectedDirectory.toAbsolutePath().toString());
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

        searchFilesTask.executeWith(Globals.TASK_EXECUTOR);
    }

    private Path getSearchDirectory() {
        Path directory = Paths.get(texDirectoryField.getText());

        if (Files.notExists(directory)) {
            directory = databaseContext.getFirstExistingFileDir(Globals.prefs.getFilePreferences())
                                       .orElse(Globals.prefs.getWorkingDir());

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
        CheckBoxTreeItem<FileNode> root = (CheckBoxTreeItem<FileNode>) filesTree.getRoot();

        if (root == null) {
            return;
        }

        List<Path> fileList = getFileListFromNode(root);

        if (fileList.isEmpty()) {
            return;
        }

        TexParserResult result = new DefaultTexParser().parse(fileList);
        ParseTexResult dialog = new ParseTexResult(result);

        dialog.showAndWait();
    }

    private List<Path> getFileListFromNode(CheckBoxTreeItem<FileNode> node) {
        List<Path> fileList = new ArrayList<>();

        for (TreeItem<FileNode> childNode : node.getChildren()) {
            CheckBoxTreeItem<FileNode> child = (CheckBoxTreeItem<FileNode>) childNode;

            if (child.isLeaf() && child.isSelected()) {
                Path nodeFile = child.getValue().getPath();

                if (nodeFile != null && Files.isRegularFile(nodeFile)) {
                    fileList.add(nodeFile);
                }
            }
        }

        return fileList;
    }
}
