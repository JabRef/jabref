package org.jabref.logic.util.io;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FileHistory {

    private static final int HISTORY_SIZE = 8;

    private final LinkedList<String> history;


    public FileHistory(List<String> fileList) {
        history = new LinkedList<>(Objects.requireNonNull(fileList));
    }

    public int size() {
        return history.size();
    }

    public boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Adds the filename to the top of the list. If it already is in the list, it is merely moved to the top.
     *
     * @param filename a <code>String</code> value
     */

    public void newFile(String filename) {
        removeItem(filename);
        history.addFirst(filename);
        while (size() > HISTORY_SIZE) {
            history.removeLast();
        }
    }

    public String getFileName(int i) {
        return history.get(i);
    }

    public void removeItem(String filename) {
        history.remove(filename);
    }

    public List<String> getHistory() {
        return history;
    }
}
