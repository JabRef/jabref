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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
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
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileNodeViewModel;
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
    private final ObjectProperty<DateRange> selectedDate = new SimpleObjectProperty<>();
    private final ObjectProperty<ExternalFileSorter> selectedSort = new SimpleObjectProperty<>();

    private final ObjectProperty<Optional<FileNodeViewModel>> treeRootProperty = new SimpleObjectProperty<>();
    private final SimpleListProperty<TreeItem<FileNodeViewModel>> checkedFileListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty taskActiveProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty progressValueProperty = new SimpleDoubleProperty(0);
    private final StringProperty progressTextProperty = new SimpleStringProperty();

    private final ObservableList<ImportFilesResultItemViewModel> resultList = FXCollections.observableArrayList();
    private final ObservableList<FileExtensionViewModel> fileFilterList;
    private final ObservableList<DateRange> dateFilterList;
    private final ObservableList<ExternalFileSorter> fileSortList;

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private BackgroundTask<FileNodeViewModel> findUnlinkedFilesTask;
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
                bibDatabase,
                externalFileTypes,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager);

        this.fileFilterList = FXCollections.observableArrayList(
            new FileExtensionViewModel(StandardFileType.ANY_FILE, externalFileTypes),
            new FileExtensionViewModel(StandardFileType.BIBTEX_DB, externalFileTypes),
            new FileExtensionViewModel(StandardFileType.PDF, externalFileTypes));

        this.dateFilterList = FXCollections.observableArrayList(DateRange.values());

        this.fileSortList = FXCollections.observableArrayList(ExternalFileSorter.values());

        Predicate<String> isDirectory = path -> Files.isDirectory(Path.of(path));
        scanDirectoryValidator = new FunctionBasedValidator<>(directoryPath, isDirectory,
                ValidationMessage.error(Localization.lang("Please enter a valid file path.")));

        treeRootProperty.setValue(Optional.empty());
    }

    public void startSearch() {
        Path directory = this.getSearchDirectory();
        Filter<Path> selectedFileFilter = selectedExtension.getValue().dirFilter();
        DateRange selectedDateFilter = selectedDate.getValue();
        ExternalFileSorter selectedSortFilter = selectedSort.getValue();
        progressValueProperty.unbind();
        progressTextProperty.unbind();

        findUnlinkedFilesTask = new UnlinkedFilesCrawler(directory, selectedFileFilter, selectedDateFilter, selectedSortFilter, bibDatabase, preferences.getFilePreferences())
                .onRunning(() -> {
                    progressValueProperty.set(ProgressIndicator.INDETERMINATE_PROGRESS);
                    progressTextProperty.setValue(Localization.lang("Searching file system..."));
                    progressTextProperty.bind(findUnlinkedFilesTask.messageProperty());
                    taskActiveProperty.setValue(true);
                    treeRootProperty.setValue(Optional.empty());
                })
                .onFinished(() -> {
                    progressValueProperty.set(0);
                    taskActiveProperty.setValue(false);
                })
                .onSuccess(treeRoot -> treeRootProperty.setValue(Optional.of(treeRoot)));

        findUnlinkedFilesTask.executeWith(taskExecutor);
    }

    public void startImport() {
        List<Path> fileList = checkedFileListProperty.stream()
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
                                                     progressValueProperty.bind(importFilesBackgroundTask.workDonePercentageProperty());
                                                     progressTextProperty.bind(importFilesBackgroundTask.messageProperty());
                                                     taskActiveProperty.setValue(true);
                                                 })
                                                 .onFinished(() -> {
                                                     progressValueProperty.unbind();
                                                     progressTextProperty.unbind();
                                                     taskActiveProperty.setValue(false);
                                                 })
                                                 .onSuccess(resultList::addAll);
        importFilesBackgroundTask.executeWith(taskExecutor);
    }

    /**
     * This starts the export of all files of all selected nodes in the file tree view.
     */
    public void startExport() {
        List<Path> fileList = checkedFileListProperty.stream()
                                                     .map(item -> item.getValue().getPath())
                                                     .filter(path -> path.toFile().isFile())
                                                     .collect(Collectors.toList());
        if (fileList.isEmpty()) {
            LOGGER.warn("There are no valid files checked");
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getWorkingDir())
                .addExtensionFilter(StandardFileType.TXT)
                .withDefaultExtension(StandardFileType.TXT)
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        if (exportPath.isEmpty()) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(exportPath.get(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE)) {
            for (Path file : fileList) {
                writer.write(file.toString() + "\n");
            }
        } catch (IOException e) {
            LOGGER.error("Error exporting", e);
        }
     }

    public ObservableList<FileExtensionViewModel> getFileFilters() {
        return this.fileFilterList;
    }

    public ObservableList<DateRange> getDateFilters() {
        return this.dateFilterList;
    }

    public ObservableList<ExternalFileSorter> getSorters() {
        return this.fileSortList;
    }

    public void cancelTasks() {
        if (findUnlinkedFilesTask != null) {
            findUnlinkedFilesTask.cancel();
        }
        if (importFilesBackgroundTask != null) {
            importFilesBackgroundTask.cancel();
        }
    }

    public void browseFileDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getWorkingDir()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(selectedDirectory -> {
                         directoryPath.setValue(selectedDirectory.toAbsolutePath().toString());
                         preferences.setWorkingDirectory(selectedDirectory.toAbsolutePath());
                     });
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

    public ObservableList<ImportFilesResultItemViewModel> resultTableItems() {
        return this.resultList;
    }

    public ObjectProperty<Optional<FileNodeViewModel>> treeRootProperty() {
        return this.treeRootProperty;
    }

    public ObjectProperty<FileExtensionViewModel> selectedExtensionProperty() {
        return this.selectedExtension;
    }

    public ObjectProperty<DateRange> selectedDateProperty() {
        return this.selectedDate;
    }

    public ObjectProperty<ExternalFileSorter> selectedSortProperty() {
        return this.selectedSort;
    }

    public StringProperty directoryPathProperty() {
        return this.directoryPath;
    }

    public ValidationStatus directoryPathValidationStatus() {
        return this.scanDirectoryValidator.getValidationStatus();
    }

    public DoubleProperty progressValueProperty() {
        return this.progressValueProperty;
    }

    public StringProperty progressTextProperty() {
        return this.progressTextProperty;
    }

    public BooleanProperty taskActiveProperty() {
        return this.taskActiveProperty;
    }

    public SimpleListProperty<TreeItem<FileNodeViewModel>> checkedFileListProperty() {
        return checkedFileListProperty;
    }
}
