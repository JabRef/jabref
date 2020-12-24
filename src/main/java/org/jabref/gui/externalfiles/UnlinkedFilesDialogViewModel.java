package org.jabref.gui.externalfiles;

import java.io.BufferedWriter;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.importer.ImportFilesResultItemViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlinkedFilesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkedFilesDialogViewModel.class);

    private final ImportHandler importHandler;
    private final StringProperty directoryPath = new SimpleStringProperty("");
    private final ObjectProperty<FileChooser.ExtensionFilter> selectedExtension = new SimpleObjectProperty<>();

    private final BooleanProperty searchProgressVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty applyButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty scanButtonDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty exportButtonDisabled = new SimpleBooleanProperty();
    private final ObjectProperty<TreeItem<FileNodeWrapper>> treeRoot = new SimpleObjectProperty<>();
    private final BooleanProperty scanButtonDefaultButton = new SimpleBooleanProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("");

    private final List<FileChooser.ExtensionFilter> fileFilterList = List.of(
                                                                             FileFilterConverter.ANY_FILE,
                                                                             FileFilterConverter.toExtensionFilter(StandardFileType.PDF),
                                                                             FileFilterConverter.toExtensionFilter(StandardFileType.BIBTEX_DB));
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private BackgroundTask<CheckBoxTreeItem<FileNodeWrapper>> findUnlinkedFilesTask;
    private BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesBackgroundTask;

    private final BibDatabaseContext bibDatabasecontext;
    private final TaskExecutor taskExecutor;

    public UnlinkedFilesDialogViewModel(DialogService dialogService, ExternalFileTypes externalFileTypes, UndoManager undoManager,
                                        FileUpdateMonitor fileUpdateMonitor, PreferencesService preferences, StateManager stateManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.bibDatabasecontext = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        importHandler = new ImportHandler(
                                          dialogService,
                                          bibDatabasecontext,
                                          externalFileTypes,
                                          preferences,
                                          fileUpdateMonitor,
                                          undoManager,
                                          stateManager);
    }

    public void startImport() {

        CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) treeRoot.getValue();
        final List<Path> fileList = getFileListFromNode(root);

        if (fileList.isEmpty()) {
            return;
        }


        importFilesBackgroundTask = importHandler.importFilesInBackground(fileList)
        .onRunning(() -> {
           // progressText.setValue(Localization.lang("Importing"));
            progress.bind(importFilesBackgroundTask.workDonePercentageProperty());
            progressText.bind(importFilesBackgroundTask.messageProperty());

            searchProgressVisible.setValue(true);
            scanButtonDisabled.setValue(true);
            applyButtonDisabled.setValue(true);
         })
        .onFinished(() -> {
            searchProgressVisible.setValue(false);
            scanButtonDisabled.setValue(false);
        })
        .onSuccess(results -> {
           applyButtonDisabled.setValue(false);
           exportButtonDisabled.setValue(false);
           scanButtonDefaultButton.setValue(false);

           searchProgressVisible.setValue(false);
           new ImportResultsDialogView(results).showAndWait();
        });
        importFilesBackgroundTask.executeWith(taskExecutor);

    }

    /**
     * This starts the export of all files of all selected nodes in the file tree view.
     */
    public void startExport() {
        CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) treeRoot.getValue();

        final List<Path> fileList = getFileListFromNode(root);
        if (fileList.isEmpty()) {
            return;
        }

        exportButtonDisabled.setValue(true);
        applyButtonDisabled.setValue(true);

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getWorkingDir()).build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        if (exportPath.isEmpty()) {
            exportButtonDisabled.setValue(false);
            applyButtonDisabled.setValue(false);
            return;
        }

        try (BufferedWriter writer =
                     Files.newBufferedWriter(exportPath.get(), StandardCharsets.UTF_8,
                             StandardOpenOption.CREATE)) {
            for (Path file : fileList) {
                writer.write(file.toString() + "\n");
            }
        } catch (IOException e) {
            LOGGER.error("Error exporting", e);
        }

        exportButtonDisabled.setValue(false);
        applyButtonDisabled.setValue(false);

    }

    public void startSearch() {

        Path directory = this.getSearchDirectory();
        FileFilter selectedFileFilter = FileFilterConverter.toFileFilter(selectedExtension.getValue());

        findUnlinkedFilesTask = new UnlinkedFilesCrawler(directory, selectedFileFilter, bibDatabasecontext)
                .onRunning(() -> {
                    progress.set(ProgressBar.INDETERMINATE_PROGRESS);
                    progressText.setValue(Localization.lang("Searching the file system...."));
                    searchProgressVisible.setValue(true);
                    scanButtonDisabled.setValue(true);
                    treeRoot.setValue(null);
                })
                .onFinished(() -> {
                    searchProgressVisible.setValue(false);
                    scanButtonDisabled.setValue(false);
                    applyButtonDisabled.setValue(false);
                })
                .onSuccess(root -> {
                    treeRoot.setValue(root);
                    root.setSelected(true);
                    root.setExpanded(true);

                   applyButtonDisabled.setValue(false);
                   exportButtonDisabled.setValue(false);
                   scanButtonDefaultButton.setValue(false);

                });
        findUnlinkedFilesTask.executeWith(taskExecutor);
    }

    public StringProperty directoryPath() {
        return this.directoryPath;
    }

    public List<FileChooser.ExtensionFilter> getFileFilters() {
        return this.fileFilterList;
    }

    public ObjectProperty<FileChooser.ExtensionFilter> selectedExtension() {
        return this.selectedExtension;
    }

    public BooleanProperty searchProgressPaneVisible() {
        return this.searchProgressVisible;
    }

    public BooleanProperty applyButtonDisabled() {
        return this.applyButtonDisabled;
    }

    public BooleanProperty scanButtonDisabled() {
        return this.scanButtonDisabled;
    }

    public BooleanProperty exportButtonDisabled() {
        return this.exportButtonDisabled;
    }

    public void browseFileDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
            .withInitialDirectory(preferences.getWorkingDir()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                 .ifPresent(selectedDirectory -> {
                     directoryPath.setValue(selectedDirectory.toAbsolutePath().toString());
                     preferences.setWorkingDirectory(selectedDirectory.toAbsolutePath());
                     this.scanButtonDisabled.setValue(false);
                 });
    }

    public ObjectProperty<TreeItem<FileNodeWrapper>> treeRoot() {
        return this.treeRoot;
    }

    public void cancelTasks() {
        if (findUnlinkedFilesTask != null) {
            findUnlinkedFilesTask.cancel();
        }
        if (importFilesBackgroundTask != null) {
            importFilesBackgroundTask.cancel();
        }
    }

    public BooleanProperty scanButtonDefaultButton() {
        return this.scanButtonDefaultButton;
    }

    public DoubleProperty progress() {
        return this.progress;
    }

    public StringProperty progressText() {
        return this.progressText;
    }

    private Path getSearchDirectory() {
        Path directory = Path.of(directoryPath.getValue());
        if (Files.notExists(directory)) {
            directory = Path.of(System.getProperty("user.dir"));
            directoryPath.setValue(directory.toAbsolutePath().toString());
        }
        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
            directoryPath.setValue(directory.toAbsolutePath().toString());
        }
        return directory;
    }

    private List<Path> getFileListFromNode(CheckBoxTreeItem<FileNodeWrapper> node) {
        List<Path> filesList = new ArrayList<>();
        for (TreeItem<FileNodeWrapper> childNode : node.getChildren()) {
            CheckBoxTreeItem<FileNodeWrapper> child = (CheckBoxTreeItem<FileNodeWrapper>) childNode;
            if (child.isLeaf()) {
                if (child.isSelected()) {
                    Path nodeFile = child.getValue().path;
                    if ((nodeFile != null) && Files.isRegularFile(nodeFile)) {
                        filesList.add(nodeFile);
                    }
                }
            } else {
                filesList.addAll(getFileListFromNode(child));
            }
        }
        return filesList;
    }

    public void selectAll() {
        CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) treeRoot.getValue();
        // Need to toggle a twice to make sure everything is selected
        root.setSelected(true);
        root.setSelected(false);
        root.setSelected(true);
    }

    public void unselectAll() {
        // TODO Auto-generated method stub
        CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) treeRoot.getValue();
        // Need to toggle a twice to make sure nothing is selected
        root.setSelected(false);
        root.setSelected(true);
        root.setSelected(false);
    }

    public void expandAll() {
        expandTree(treeRoot.getValue(), true);
        treeRoot.getValue().setExpanded(true);
    }

    public void collapseAll() {
        expandTree(treeRoot.getValue(), false);
        treeRoot.getValue().setExpanded(false);
    }

    /**
     * Expands or collapses the specified tree according to the <code>expand</code>-parameter.
     */
    private void expandTree(TreeItem<?> item, boolean expand) {
        if ((item != null) && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandTree(child, expand);
            }
        }
    }
}
