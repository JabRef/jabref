package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
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
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class ParseTexDialogViewModel extends AbstractViewModel {

    private static final String TEX_EXT = ".tex";
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final Path initialPath;
    private final ObjectProperty<FileNodeViewModel> root;
    private final ObservableList<TreeItem<FileNodeViewModel>> checkedFileList;
    private final StringProperty texDirectory;
    private final BooleanProperty noFilesFound;
    private final BooleanProperty searchInProgress;
    private final BooleanProperty successfulSearch;

    public ParseTexDialogViewModel(BibDatabaseContext databaseContext, DialogService dialogService,
                                   TaskExecutor taskExecutor, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.initialPath = databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                          .orElse(preferencesService.getWorkingDir());

        this.root = new SimpleObjectProperty<>();
        this.checkedFileList = FXCollections.observableArrayList();
        this.texDirectory = new SimpleStringProperty("");
        this.noFilesFound = new SimpleBooleanProperty(true);
        this.searchInProgress = new SimpleBooleanProperty(false);
        this.successfulSearch = new SimpleBooleanProperty(false);

        texDirectory.set(initialPath.toAbsolutePath().toString());
    }

    public ObjectProperty<FileNodeViewModel> rootProperty() {
        return root;
    }

    public ObservableList<TreeItem<FileNodeViewModel>> getCheckedFileList() {
        return checkedFileList;
    }

    public StringProperty texDirectoryProperty() {
        return texDirectory;
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

    protected void browseButtonClicked() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getSearchDirectory()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory -> {
            texDirectory.set(selectedDirectory.toAbsolutePath().toString());
            preferencesService.setWorkingDir(selectedDirectory.toAbsolutePath());
        });
    }

    private Path getSearchDirectory() {
        Path directory = Paths.get(texDirectory.get());

        if (Files.notExists(directory)) {
            directory = initialPath;
            texDirectory.set(directory.toAbsolutePath().toString());
        }

        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
            texDirectory.set(directory.toAbsolutePath().toString());
        }

        return directory;
    }

    protected void searchButtonClicked() {
        BackgroundTask.wrap(() -> searchDirectory(getSearchDirectory()))
                      .onRunning(() -> {
                          noFilesFound.set(true);
                          searchInProgress.set(true);
                          successfulSearch.set(false);
                      })
                      .onFinished(() -> searchInProgress.set(false))
                      .onSuccess(root -> {
                          this.root.set(root);
                          noFilesFound.set(false);
                          successfulSearch.set(true);
                      })
                      .onFailure(dialogService::showErrorDialogAndWait)
                      .executeWith(taskExecutor);
    }

    private FileNodeViewModel searchDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IOException();
        }

        List<Path> files = Files.list(directory)
                                .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(TEX_EXT))
                                .collect(Collectors.toList());

        List<Path> subDirectories = Files.list(directory)
                                         .filter(path -> Files.isDirectory(path))
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

    protected void parseButtonClicked() {
        List<Path> fileList = checkedFileList.stream()
                                             .map(item -> item.getValue().getPath().toAbsolutePath())
                                             .filter(path -> path != null && Files.isRegularFile(path))
                                             .collect(Collectors.toList());
        if (fileList.isEmpty()) {
            return;
        }

        BackgroundTask.wrap(() -> new DefaultTexParser().parse(fileList))
                      .onSuccess(result -> new ParseTexResultView(result).showAndWait())
                      .onFailure(dialogService::showErrorDialogAndWait)
                      .executeWith(taskExecutor);
    }
}
