package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FileHistory {

    private static final int HISTORY_SIZE = 8;

    private final ObservableList<Path> history;

    public FileHistory(List<Path> files) {
        history = FXCollections.observableList(Objects.requireNonNull(files));
    }

    public int size() {
        return history.size();
    }

    public boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Adds the file to the top of the list. If it already is in the list, it is merely moved to the top.
     */
    public void newFile(Path file) {
        removeItem(file);
        history.add(0, file);
        while (size() > HISTORY_SIZE) {
            history.remove(HISTORY_SIZE);
        }
    }

    public Path getFileAt(int index) {
        return history.get(index);
    }

    public void removeItem(Path file) {
        history.remove(file);
    }

    public ObservableList<Path> getHistory() {
        return history;
    }
}
