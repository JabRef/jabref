package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.preferences.PreferencesService;

public class ParseTexDialogViewModel extends AbstractViewModel {

    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final Path initialPath;
    private final ObservableList<Path> searchPathList = FXCollections.observableArrayList();
    private final ObservableList<Path> selectedFileList = FXCollections.observableArrayList();
    private final StringProperty texDirectory = new SimpleStringProperty("");
    private final BooleanProperty browseButton = new SimpleBooleanProperty(false);
    private final BooleanProperty searchButton = new SimpleBooleanProperty(false);
    private final BooleanProperty selectAllButton = new SimpleBooleanProperty(true);
    private final BooleanProperty unselectAllButton = new SimpleBooleanProperty(true);
    private final BooleanProperty parseButton = new SimpleBooleanProperty(true);

    public ParseTexDialogViewModel(BibDatabaseContext databaseContext, DialogService dialogService,
                                   TaskExecutor taskExecutor, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.initialPath = databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                          .orElse(preferencesService.getWorkingDir());

        this.texDirectory.set(initialPath.toAbsolutePath().toString());
    }

    protected StringProperty texDirectoryProperty() {
        return texDirectory;
    }

    protected BooleanProperty browseButtonProperty() {
        return browseButton;
    }

    protected BooleanProperty searchButtonProperty() {
        return searchButton;
    }

    protected ObservableList<Path> getSearchPathList() {
        return searchPathList;
    }

    protected ObservableList<Path> getSelectedFileList() {
        return selectedFileList;
    }

    protected BooleanProperty selectAllButtonProperty() {
        return selectAllButton;
    }

    protected BooleanProperty unselectAllButtonProperty() {
        return unselectAllButton;
    }

    protected BooleanProperty parseButtonProperty() {
        return parseButton;
    }

    protected void browseButtonClicked() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(initialPath).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory -> {
            texDirectory.set(selectedDirectory.toAbsolutePath().toString());
            preferencesService.setWorkingDir(selectedDirectory.toAbsolutePath());
        });
    }

    protected void searchButtonClicked() {
        BackgroundTask walkFileTreeTask = new WalkFileTreeView(getSearchDirectory())
                .onRunning(() -> {
                    browseButton.set(true);
                    searchButton.set(true);
                    selectAllButton.set(true);
                    unselectAllButton.set(true);
                    parseButton.set(true);
                })
                .onFinished(() -> {
                    browseButton.set(false);
                    searchButton.set(false);
                })
                .onSuccess(paths -> {
                    searchPathList.setAll(paths);
                    selectAllButton.set(false);
                    unselectAllButton.set(false);
                    parseButton.set(false);
                });

        walkFileTreeTask.executeWith(taskExecutor);
    }

    protected Path getSearchDirectory() {
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

    protected void parse() {
        if (selectedFileList == null) {
            return;
        }

        List<Path> fileList = selectedFileList.stream()
                                              .map(path -> path.toAbsolutePath())
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
