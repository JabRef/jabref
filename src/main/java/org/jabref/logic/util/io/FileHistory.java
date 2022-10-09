package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ModifiableObservableListBase;

public class FileHistory extends ModifiableObservableListBase<Path> {

    private static final int HISTORY_SIZE = 8;

    private final List<Path> history;

    private FileHistory(List<Path> list) {
        history = new ArrayList<>(list);
    }

    @Override
    public Path get(int index) {
        return history.get(index);
    }

    public int size() {
        return history.size();
    }

    @Override
    protected void doAdd(int index, Path element) {
        history.add(index, element);
    }

    @Override
    protected Path doSet(int index, Path element) {
        return history.set(index, element);
    }

    @Override
    protected Path doRemove(int index) {
        return history.remove(index);
    }

    /**
     * Adds the file to the top of the list. If it already is in the list, it is merely moved to the top.
     */
    public void newFile(Path file) {
        removeItem(file);
        this.add(0, file);
        while (size() > HISTORY_SIZE) {
            history.remove(HISTORY_SIZE);
        }
    }

    public void removeItem(Path file) {
        this.remove(file);
    }

    public static FileHistory of(List<Path> list) {
        return new FileHistory(new ArrayList<>(list));
    }
}
