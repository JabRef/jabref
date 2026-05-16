package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ModifiableObservableListBase;

import org.jabref.logic.util.JabRefBaseDirectoryLocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHistory extends ModifiableObservableListBase<Path> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileHistory.class);

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

    /// Adds the file to the top of the list. If it already is in the list, it is merely moved to the top.
    public void newFile(Path file) {
        removeItem(file);
        this.addFirst(file);
        while (size() > HISTORY_SIZE) {
            history.remove(HISTORY_SIZE);
        }
    }

    public void removeItem(Path file) {
        this.remove(file);

        Path baseDirectoryPath = JabRefBaseDirectoryLocator.getBaseDirectoryPath();

        // The history may contain both absolute and base-directory-relative paths,
        // depending on how the file was added previously. Remove all equivalent
        // representations to ensure the entry is fully cleared from the history.
        if (file.isAbsolute()) {
            try {
                this.remove(baseDirectoryPath.relativize(file).normalize());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Could not relativize file path: {}", file, e);
                return;
            }
            return;
        }

        this.remove(baseDirectoryPath.resolve(file).normalize());
    }

    public static FileHistory of(List<Path> list) {
        return new FileHistory(new ArrayList<>(list));
    }
}
