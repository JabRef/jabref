package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FileHistory {

    private static final int HISTORY_SIZE = 8;

    private final LinkedList<Path> history;

    public FileHistory(List<Path> files) {
        history = new LinkedList<>(Objects.requireNonNull(files));
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
        history.addFirst(file);
        while (size() > HISTORY_SIZE) {
            history.removeLast();
        }
    }

    public Path getFileAt(int index) {
        return history.get(index);
    }

    public void removeItem(Path file) {
        history.remove(file);
    }

    public List<Path> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
