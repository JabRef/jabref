package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.logic.texparser.TexBibEntriesResolver;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseTexDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseTexDialogViewModel.class);
    private static final String TEX_EXT = ".tex";
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileMonitor;
    private final StringProperty texDirectory;
    private final Validator texDirectoryValidator;
    private final ObjectProperty<FileNodeViewModel> root;
    private final ObservableList<TreeItem<FileNodeViewModel>> checkedFileList;
    private final BooleanProperty noFilesFound;
    private final BooleanProperty searchInProgress;
    private final BooleanProperty successfulSearch;

    public ParseTexDialogViewModel(BibDatabaseContext databaseContext, DialogService dialogService,
                                   TaskExecutor taskExecutor, PreferencesService preferencesService,
                                   FileUpdateMonitor fileMonitor) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.fileMonitor = fileMonitor;
        this.texDirectory = new SimpleStringProperty(databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                                                    .orElseGet(preferencesService::getWorkingDir)
                                                                    .toAbsolutePath().toString());
        this.root = new SimpleObjectProperty<>();
        this.checkedFileList = FXCollections.observableArrayList();
        this.noFilesFound = new SimpleBooleanProperty(true);
        this.searchInProgress = new SimpleBooleanProperty(false);
        this.successfulSearch = new SimpleBooleanProperty(false);

        Predicate<String> isDirectory = path -> Paths.get(path).toFile().isDirectory();
        texDirectoryValidator = new FunctionBasedValidator<>(texDirectory, isDirectory,
                ValidationMessage.error(Localization.lang("Please enter a valid file path.")));
    }

    public StringProperty texDirectoryProperty() {
        return texDirectory;
    }

    public ValidationStatus texDirectoryValidation() {
        return texDirectoryValidator.getValidationStatus();
    }

    public ObjectProperty<FileNodeViewModel> rootProperty() {
        return root;
    }

    public ObservableList<TreeItem<FileNodeViewModel>> getCheckedFileList() {
        return new ReadOnlyListWrapper<>(checkedFileList);
    }

    public BooleanProperty noFilesFoundProperty() {
        return noFilesFound;
    }

    public BooleanProperty searchInProgressProperty() {
        return searchInProgress;
    }

    public BooleanProperty successfulSearchProperty() {
        return successfulSearch;
    }

    public void browseButtonClicked() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Paths.get(texDirectory.get())).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory -> {
            texDirectory.set(selectedDirectory.toAbsolutePath().toString());
            preferencesService.setWorkingDir(selectedDirectory.toAbsolutePath());
        });
    }

    /**
     * Run a recursive search in a background task.
     */
    public void searchButtonClicked() {
        BackgroundTask.wrap(() -> searchDirectory(Paths.get(texDirectory.get())))
                      .onRunning(() -> {
                          root.set(null);
                          noFilesFound.set(true);
                          searchInProgress.set(true);
                          successfulSearch.set(false);
                      })
                      .onFinished(() -> searchInProgress.set(false))
                      .onSuccess(newRoot -> {
                          root.set(newRoot);
                          noFilesFound.set(false);
                          successfulSearch.set(true);
                      })
                      .onFailure(this::handleFailure)
                      .executeWith(taskExecutor);
    }

    private void handleFailure(Exception exception) {
        final boolean permissionProblem = exception instanceof IOException && exception.getCause() instanceof FileSystemException && exception.getCause().getMessage().endsWith("Operation not permitted");
        if (permissionProblem) {
            dialogService.showErrorDialogAndWait(String.format(Localization.lang("JabRef does not have permission to access %s"), exception.getCause().getMessage()));
        } else {
            dialogService.showErrorDialogAndWait(exception);
        }
    }

    private FileNodeViewModel searchDirectory(Path directory) throws IOException {
        if (directory == null || !directory.toFile().isDirectory()) {
            throw new IOException(String.format("Invalid directory for searching: %s", directory));
        }

        FileNodeViewModel parent = new FileNodeViewModel(directory);
        Map<Boolean, List<Path>> fileListPartition;

        try (Stream<Path> filesStream = Files.list(directory)) {
            fileListPartition = filesStream.collect(Collectors.partitioningBy(path -> path.toFile().isDirectory()));
        } catch (IOException e) {
            LOGGER.error(String.format("%s while searching files: %s", e.getClass().getName(), e.getMessage()));
            return parent;
        }

        List<Path> subDirectories = fileListPartition.get(true);
        List<Path> files = fileListPartition.get(false)
                                            .stream()
                                            .filter(path -> path.toString().endsWith(TEX_EXT))
                                            .collect(Collectors.toList());
        int fileCount = 0;

        for (Path subDirectory : subDirectories) {
            FileNodeViewModel subRoot = searchDirectory(subDirectory);

            if (!subRoot.getChildren().isEmpty()) {
                fileCount += subRoot.getFileCount();
                parent.getChildren().add(subRoot);
            }
        }

        parent.setFileCount(files.size() + fileCount);
        parent.getChildren().addAll(files.stream()
                                         .map(FileNodeViewModel::new)
                                         .collect(Collectors.toList()));
        return parent;
    }

    /**
     * Parse all checked files in a background task.
     */
    public void parseButtonClicked() {
        List<Path> fileList = checkedFileList.stream()
                                             .map(item -> item.getValue().getPath())
                                             .filter(path -> path.toFile().isFile())
                                             .collect(Collectors.toList());
        if (fileList.isEmpty()) {
            LOGGER.warn("There are no valid files checked");
            return;
        }

        TexBibEntriesResolver entriesResolver = new TexBibEntriesResolver(databaseContext.getDatabase(),
                preferencesService.getImportFormatPreferences(), fileMonitor);

        BackgroundTask.wrap(() -> entriesResolver.resolve(new DefaultTexParser().parse(fileList)))
                      .onRunning(() -> searchInProgress.set(true))
                      .onFinished(() -> searchInProgress.set(false))
                      .onSuccess(result -> new ParseTexResultView(result, databaseContext, Paths.get(texDirectory.get())).showAndWait())
                      .onFailure(dialogService::showErrorDialogAndWait)
                      .executeWith(taskExecutor);
    }
}
