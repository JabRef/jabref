package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.util.io.FileHistory;

public class LastFilesOpenedPreferences {

    // the last libraries that were open when jabref closes and should be reopened on startup
    private final ObservableList<Path> lastFilesOpened;

    private final ObjectProperty<Path> lastFocusedFile;

    // observable list last files opened in the file menu
    private final FileHistory fileHistory;

    public LastFilesOpenedPreferences(List<Path> lastFilesOpened, Path lastFocusedFile, FileHistory fileHistory) {
        this.lastFilesOpened = FXCollections.observableArrayList(lastFilesOpened);
        this.lastFocusedFile = new SimpleObjectProperty<>(lastFocusedFile);
        this.fileHistory = fileHistory;
    }

    public ObservableList<Path> getLastFilesOpened() {
        return lastFilesOpened;
    }

    public void setLastFilesOpened(List<Path> files) {
        lastFilesOpened.setAll(files);
    }

    public Path getLastFocusedFile() {
        return lastFocusedFile.get();
    }

    public ObjectProperty<Path> lastFocusedFileProperty() {
        return lastFocusedFile;
    }

    public void setLastFocusedFile(Path lastFocusedFile) {
        this.lastFocusedFile.set(lastFocusedFile);
    }

    public FileHistory getFileHistory() {
        return fileHistory;
    }
}
