package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;

public class ParseTexDialogViewModel extends AbstractViewModel {

    private static final String TEX_EXT = ".tex";
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final StringProperty texDirectory;
    private final FunctionBasedValidator texDirectoryValidator;
    private final ObjectProperty<FileNodeViewModel> root;
    private final ObservableList<TreeItem<FileNodeViewModel>> checkedFileList;
    private final BooleanProperty noFilesFound;
    private final BooleanProperty searchInProgress;
    private final BooleanProperty successfulSearch;

    public ParseTexDialogViewModel(BibDatabaseContext databaseContext, DialogService dialogService,
                                   TaskExecutor taskExecutor, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.texDirectory = new SimpleStringProperty("");
        this.root = new SimpleObjectProperty<>();
        this.checkedFileList = FXCollections.observableArrayList();
        this.noFilesFound = new SimpleBooleanProperty(true);
        this.searchInProgress = new SimpleBooleanProperty(false);
        this.successfulSearch = new SimpleBooleanProperty(false);

        texDirectory.set(databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                        .orElse(preferencesService.getWorkingDir())
                                        .toAbsolutePath().toString());
        Predicate<String> itExists = path -> (path != null) && Paths.get(path).toFile().exists();
        Predicate<String> isDirectory = path -> Paths.get(path).toFile().isDirectory();
        Predicate<String> isDirectoryAndExists = itExists.and(isDirectory);
        texDirectoryValidator = new FunctionBasedValidator<>(texDirectory, isDirectoryAndExists,
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
                      .onFailure(dialogService::showErrorDialogAndWait)
                      .executeWith(taskExecutor);
    }

    private FileNodeViewModel searchDirectory(Path directory) throws IOException {
        if (directory == null || !directory.toFile().exists() || !directory.toFile().isDirectory()) {
            throw new IOException();
        }

        List<Path> files = Files.list(directory)
                                .filter(path -> !path.toFile().isDirectory() && path.toString().endsWith(TEX_EXT))
                                .collect(Collectors.toList());

        List<Path> subDirectories = Files.list(directory)
                                         .filter(path -> path.toFile().isDirectory())
                                         .collect(Collectors.toList());

        FileNodeViewModel parent = new FileNodeViewModel(directory);
        int fileCount = 0;

        for (Path subDirectory : subDirectories) {
            FileNodeViewModel subRoot = searchDirectory(subDirectory);

            if (subRoot != null && !subRoot.getChildren().isEmpty()) {
                fileCount += subRoot.getFileCount();
                parent.getChildren().add(subRoot);
            }
        }

        parent.setFileCount(files.size() + fileCount);
        files.forEach(file -> parent.getChildren().add(new FileNodeViewModel(file)));

        return parent;
    }

    public void parseButtonClicked() {
        List<Path> fileList = checkedFileList.stream()
                                             .map(item -> item.getValue().getPath().toAbsolutePath())
                                             .filter(path -> path != null && path.toFile().isFile())
                                             .collect(Collectors.toList());
        if (fileList.isEmpty()) {
            return;
        }

        BackgroundTask.wrap(() -> new DefaultTexParser().parse(fileList))
                      .onRunning(() -> searchInProgress.set(true))
                      .onFinished(() -> searchInProgress.set(false))
                      .onSuccess(result -> new ParseTexResultView(result).showAndWait())
                      .onFailure(dialogService::showErrorDialogAndWait)
                      .executeWith(taskExecutor);
    }
}
