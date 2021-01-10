package org.jabref.gui.externalfiles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.texparser.FileNodeViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlinkedFilesDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkedFilesDialogViewModel.class);

    private final ImportHandler importHandler;
    private final StringProperty directoryPath = new SimpleStringProperty("");
    private final ObjectProperty<FileExtensionViewModel> selectedExtension = new SimpleObjectProperty<>();

    private final BooleanProperty searchProgressVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty applyButtonDisabled = new SimpleBooleanProperty();
    private final BooleanProperty scanButtonDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty exportButtonDisabled = new SimpleBooleanProperty();
    private final ObjectProperty<FileNodeViewModel> treeRoot = new SimpleObjectProperty<>();
    private final ObservableList<TreeItem<FileNodeViewModel>> checkedFileList = FXCollections.observableArrayList();

    private final BooleanProperty scanButtonDefaultButton = new SimpleBooleanProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty();
    private final BooleanProperty filePaneExpanded = new SimpleBooleanProperty();
    private final BooleanProperty resultPaneExpanded = new SimpleBooleanProperty();

    private final ObservableList<ImportFilesResultItemViewModel> resultList = FXCollections.observableArrayList();
    private final ObservableList<FileExtensionViewModel> fileFilterList;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private BackgroundTask<org.jabref.gui.texparser.FileNodeViewModel> findUnlinkedFilesTask;
    private BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesBackgroundTask;

    private final BibDatabaseContext bibDatabase;
    private final TaskExecutor taskExecutor;

    private final FunctionBasedValidator<String> scanDirectoryValidator;

    public UnlinkedFilesDialogViewModel(DialogService dialogService, ExternalFileTypes externalFileTypes, UndoManager undoManager,
                                        FileUpdateMonitor fileUpdateMonitor, PreferencesService preferences, StateManager stateManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.bibDatabase = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        importHandler = new ImportHandler(
                                          dialogService,
                                          bibDatabase,
                                          externalFileTypes,
                                          preferences,
                                          fileUpdateMonitor,
                                          undoManager,
                                          stateManager);

       this.fileFilterList = FXCollections.observableArrayList(
                                            new FileExtensionViewModel(StandardFileType.ANY_FILE,externalFileTypes),
                                            new FileExtensionViewModel(StandardFileType.BIBTEX_DB, externalFileTypes),
                                            new FileExtensionViewModel(StandardFileType.PDF, externalFileTypes));


       Predicate<String> isDirectory = path -> Files.isDirectory(Path.of(path));
       scanDirectoryValidator = new FunctionBasedValidator<>(directoryPath, isDirectory,
               ValidationMessage.error(Localization.lang("Please enter a valid file path.")));
    }

    public void startImport() {
        List<Path> fileList = checkedFileList.stream()
                                             .map(item -> item.getValue().getPath())
                                             .filter(path -> path.toFile().isFile())
                                             .collect(Collectors.toList());
        if (fileList.isEmpty()) {
            LOGGER.warn("There are no valid files checked");
            return;
        }
        resultList.clear();

        importFilesBackgroundTask = importHandler.importFilesInBackground(fileList)
        .onRunning(() -> {
            progress.bind(importFilesBackgroundTask.workDonePercentageProperty());
            progressText.bind(importFilesBackgroundTask.messageProperty());

            searchProgressVisible.setValue(true);
            scanButtonDisabled.setValue(true);
            applyButtonDisabled.setValue(true);
         })
        .onFinished(() -> {
            progress.unbind();
            progressText.unbind();
            searchProgressVisible.setValue(false);
            scanButtonDisabled.setValue(false);
        })
        .onSuccess(results -> {
           applyButtonDisabled.setValue(false);
           exportButtonDisabled.setValue(false);
           scanButtonDefaultButton.setValue(false);

           progress.unbind();
           progressText.unbind();
           searchProgressVisible.setValue(false);

           filePaneExpanded.setValue(false);
           resultPaneExpanded.setValue(true);
           resultList.addAll(results);
        });
        importFilesBackgroundTask.executeWith(taskExecutor);
    }

    /**
     * This starts the export of all files of all selected nodes in the file tree view.
     */
    public void startExport() {
        List<Path> fileList = checkedFileList.stream()
                                             .map(item -> item.getValue().getPath())
                                             .filter(path -> path.toFile().isFile())
                                             .collect(Collectors.toList());
        if (fileList.isEmpty()) {
            LOGGER.warn("There are no valid files checked");
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
        Filter<Path> selectedFileFilter = selectedExtension.getValue().dirFilter();

        filePaneExpanded.setValue(true);
        resultPaneExpanded.setValue(false);

        progress.unbind();
        progressText.unbind();

        findUnlinkedFilesTask = new UnlinkedFilesCrawler(directory, selectedFileFilter, bibDatabase, preferences.getFilePreferences())
        .onRunning(() -> {

            progress.set(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressText.setValue(Localization.lang("Searching file system..."));
            progressText.bind(findUnlinkedFilesTask.messageProperty());

            searchProgressVisible.setValue(true);
            scanButtonDisabled.setValue(true);
            applyButtonDisabled.set(true);
            treeRoot.setValue(null);
        })
        .onFinished(() -> {
            scanButtonDisabled.setValue(false);
            applyButtonDisabled.setValue(false);
            searchProgressVisible.set(false);
            progress.set(0);
            searchProgressVisible.set(false);

        })
        .onSuccess(root -> {
            treeRoot.setValue(root);
            progress.set(0);

            applyButtonDisabled.setValue(false);
            exportButtonDisabled.setValue(false);
            scanButtonDefaultButton.setValue(false);
            searchProgressVisible.set(false);
        });
       findUnlinkedFilesTask.executeWith(taskExecutor);
    }

    public StringProperty directoryPath() {
        return this.directoryPath;
    }

    public ObservableList<FileExtensionViewModel> getFileFilters() {
        return this.fileFilterList;
    }

    public ObjectProperty<FileExtensionViewModel> selectedExtension() {
        return this.selectedExtension;
    }

    public BooleanProperty searchProgressVisible() {
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

    public ObjectProperty<FileNodeViewModel> treeRoot() {
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

    public void selectAll() {
        // Need to toggle a twice to make sure everything is selected

    }

    public void unselectAll() {
        // Need to toggle a twice to make sure nothing is selected

    }

    public void expandAll() {

    }

    public void collapseAll() {

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

    public ObservableList<ImportFilesResultItemViewModel> resultTableItems() {
        return this.resultList;
    }

    public BooleanProperty filePaneExpanded() {
        return this.filePaneExpanded;
    }

    public BooleanProperty resultPaneExpanded() {
        return this.resultPaneExpanded;
    }

    public ObservableList<TreeItem<FileNodeViewModel>> getCheckedFileList() {
        return new ReadOnlyListWrapper<>(checkedFileList);
    }

    public ValidationStatus directoryPathValidator() {
        return this.scanDirectoryValidator.getValidationStatus();
    }

}
