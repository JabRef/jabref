package org.jabref.gui.texparser;

import java.nio.file.Path;
import java.util.StringJoiner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;

class FileNodeViewModel {

    private final Path path;
    private final ObservableList<FileNodeViewModel> children;
    private int fileCount;

    public FileNodeViewModel(Path path) {
        this.path = path;
        this.fileCount = 0;
        this.children = FXCollections.observableArrayList();
    }

    public Path getPath() {
        return path;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public ObservableList<FileNodeViewModel> getChildren() {
        return children;
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
        return new StringJoiner(", ", FileNodeViewModel.class.getSimpleName() + "[", "]")
                .add("path=" + path)
                .add("fileCount=" + fileCount)
                .add("children=" + children)
                .toString();
    }
}
