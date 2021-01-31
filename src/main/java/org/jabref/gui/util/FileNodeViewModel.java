package org.jabref.gui.util;

import java.nio.file.Path;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;

public class FileNodeViewModel {

    private final Path path;
    private final ObservableList<FileNodeViewModel> children;
    private int fileCount;

    public FileNodeViewModel(Path path) {
        this.path = path;
        this.children = FXCollections.observableArrayList();
        this.fileCount = 0;
    }

    public Path getPath() {
        return path;
    }

    public ObservableList<FileNodeViewModel> getChildren() {
        return new ReadOnlyListWrapper<>(children);
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    /**
     * Return a string for displaying a node name (and its number of children if it is a directory).
     */
    public String getDisplayText() {
        if (path.toFile().isDirectory()) {
            return String.format("%s (%s %s)", path.getFileName(), fileCount,
                    fileCount == 1 ? Localization.lang("file") : Localization.lang("files"));
        }
        return path.getFileName().toString();
    }

    @Override
    public String toString() {
        return String.format("FileNodeViewModel{path=%s, children=%s, fileCount=%s}",
                this.path,
                this.children,
                this.fileCount);
    }
}
