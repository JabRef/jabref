package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.util.io.FileHistory;

import org.jspecify.annotations.Nullable;

public class LastFilesOpenedPreferences {

    // the last libraries that were open when jabref closes and should be reopened on startup
    private final ObservableList<Path> lastFilesOpened;

    private final ObjectProperty<Path> lastFocusedFile;

    // shared databases (by their id in SharedDatabasePreferences) that were connected without a local file when jabref closed
    private final ObservableList<String> lastSharedDatabasesOpened;

    // observable list last files opened in the file menu
    private final FileHistory fileHistory;

    private LastFilesOpenedPreferences() {
        this(
                List.of(),                // No last files opened on startup
                null,                     // No last focused file
                List.of(),                // No shared databases connected
                FileHistory.of(List.of()) // Empty file history
        );
    }

    public LastFilesOpenedPreferences(List<Path> lastFilesOpened,
                                      @Nullable Path lastFocusedFile,
                                      List<String> lastSharedDatabasesOpened,
                                      FileHistory fileHistory) {
        this.lastFilesOpened = FXCollections.observableArrayList(lastFilesOpened);
        this.lastFocusedFile = new SimpleObjectProperty<>(lastFocusedFile);
        this.lastSharedDatabasesOpened = FXCollections.observableArrayList(lastSharedDatabasesOpened);
        this.fileHistory = fileHistory;
    }

    public static LastFilesOpenedPreferences getDefault() {
        return new LastFilesOpenedPreferences();
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

    public ObservableList<String> getLastSharedDatabasesOpened() {
        return lastSharedDatabasesOpened;
    }

    public void setLastSharedDatabasesOpened(List<String> sharedDatabaseIds) {
        lastSharedDatabasesOpened.setAll(sharedDatabaseIds);
    }

    public FileHistory getFileHistory() {
        return fileHistory;
    }
}
