package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

    /// Citation keys of the entries that were selected in the libraries of [#lastFilesOpened], by the same index.
    /// An empty string means "nothing to restore" (no selection, or an entry without a citation key).
    private final ObservableList<String> lastSelectedEntries;

    // observable list last files opened in the file menu
    private final FileHistory fileHistory;

    private LastFilesOpenedPreferences() {
        this(
                List.of(),                // No last files opened on startup
                List.of(),                // No last selected entries
                null,                     // No last focused file
                FileHistory.of(List.of()) // Empty file history
        );
    }

    public LastFilesOpenedPreferences(List<Path> lastFilesOpened,
                                      List<String> lastSelectedEntries,
                                      @Nullable Path lastFocusedFile,
                                      FileHistory fileHistory) {
        this.lastFilesOpened = FXCollections.observableArrayList(lastFilesOpened);
        this.lastSelectedEntries = FXCollections.observableArrayList(lastSelectedEntries);
        this.lastFocusedFile = new SimpleObjectProperty<>(lastFocusedFile);
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

    public ObservableList<String> getLastSelectedEntries() {
        return lastSelectedEntries;
    }

    public void setLastSelectedEntries(List<String> citationKeys) {
        lastSelectedEntries.setAll(citationKeys);
    }

    /// @return the citation key that was selected in the given library when JabRef was last closed
    public Optional<String> getLastSelectedEntry(Path file) {
        int index = lastFilesOpened.indexOf(file);
        if ((index < 0) || (index >= lastSelectedEntries.size())) {
            return Optional.empty();
        }
        return Optional.of(lastSelectedEntries.get(index)).filter(key -> !key.isEmpty());
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
