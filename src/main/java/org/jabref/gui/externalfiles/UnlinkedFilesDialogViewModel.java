package org.jabref.gui.externalfiles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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
import javafx.scene.input.TransferMode;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateMonitor;

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
    private final CliPreferences preferences;
    private BackgroundTask<FileNodeViewModel> findUnlinkedFilesTask;
    private BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesBackgroundTask;

    private final BibDatabaseContext bibDatabase;
    private final TaskExecutor taskExecutor;

    private final FunctionBasedValidator<String> scanDirectoryValidator;

    private List<BibEntry> entriesToImportWithoutDuplicates;

    public UnlinkedFilesDialogViewModel(DialogService dialogService,
                                        UndoManager undoManager,
                                        FileUpdateMonitor fileUpdateMonitor,
                                        GuiPreferences preferences,
                                        StateManager stateManager,
                                        TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.bibDatabase = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        importHandler = new ImportHandler(
                bibDatabase,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);

        this.fileFilterList = FXCollections.observableArrayList(
                new FileExtensionViewModel(StandardFileType.ANY_FILE, preferences.getExternalApplicationsPreferences()),
                new FileExtensionViewModel(StandardFileType.HTML, preferences.getExternalApplicationsPreferences()),
                new FileExtensionViewModel(StandardFileType.MARKDOWN, preferences.getExternalApplicationsPreferences()),
                new FileExtensionViewModel(StandardFileType.PDF, preferences.getExternalApplicationsPreferences()));

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


        List<BibEntry> entriesToImport = importHandler.getEntriesToImport(fileList);

        entriesToImportWithoutDuplicates = new ArrayList<>();

        for (BibEntry entry : entriesToImport) {
            Optional<BibEntry> existingDuplicate = importHandler.findDuplicate(bibDatabase, entry);
            if (existingDuplicate.isPresent()) {
                LOGGER.info("Skipping duplicate entry: {}", entry);
            } else {
                entriesToImportWithoutDuplicates.add(entry);
            }
        }

        if (entriesToImportWithoutDuplicates.isEmpty()) {
            LOGGER.warn("No unique entries to import, skipping background task.");
            return;
        }
        importFilesBackgroundTask = importHandler.importFilesInBackground(fileList, bibDatabase, preferences.getFilePreferences(), TransferMode.LINK)
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
                                                 .onSuccess(new Consumer<List<ImportFilesResultItemViewModel>>() {
                                                     @Override
                                                     public void accept(List<ImportFilesResultItemViewModel> importFilesResultItemViewModels) {
                                                         if (entriesToImportWithoutDuplicates != null && !entriesToImportWithoutDuplicates.isEmpty()) {
                                                             List<ImportFilesResultItemViewModel> convertedEntries = entriesToImportWithoutDuplicates.stream()
                                                                                                                                                     .map(entry -> new ImportFilesResultItemViewModel(
                                                                                                                                                             Objects.requireNonNull(UnlinkedFilesDialogViewModel.this.getFilePathFromEntry(entry)),
                                                                                                                                                             true,
                                                                                                                                                             "Imported successfully"))
                                                                                                                                                     .toList();
                                                             resultList.addAll(convertedEntries);
                                                         } else {
                                                             LOGGER.warn("No entries to import or list is not initialized properly.");
                                                         }
                                                     }
                                                 });

        importFilesBackgroundTask.executeWith(taskExecutor);
    }

    /**
     * Extracts the file path from the given BibEntry.
     *
     * @param entry The BibEntry instance from which to extract the file path
     * @return The file path if a valid file field exists in the BibEntry; otherwise, returns an empty path.
     *
     */
    private Path getFilePathFromEntry(BibEntry entry) {
        Optional<String> fileField = entry.getField(StandardField.FILE);
        if (fileField.isPresent()) {
            String fileFieldValue = fileField.get();
            String[] fileParts = fileFieldValue.split(":");

            if (fileParts.length > 1) {
                return Path.of(fileParts[1]);
            } else {
                LOGGER.warn("File field is not in the expected format: {}", fileFieldValue);
            }
        } else {
            LOGGER.warn("No file associated with the entry: {}", entry);
        }
        return Path.of("");
    }

    /**
     * This starts the export of all files of all selected nodes in the file tree view.
     */
    public void startExport() {
        List<Path> fileList = checkedFileListProperty.stream()
                                                     .map(item -> item.getValue().getPath())
                                                     .filter(path -> path.toFile().isFile())
                                                     .toList();
        if (fileList.isEmpty()) {
            LOGGER.warn("There are no valid files checked");
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
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
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(selectedDirectory -> {
                         directoryPath.setValue(selectedDirectory.toAbsolutePath().toString());
                         preferences.getFilePreferences().setWorkingDirectory(selectedDirectory.toAbsolutePath());
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
