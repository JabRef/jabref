package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;

public class FileNodeViewModel {

    private final FileNode fileNode;
    private final ObservableList<FileNodeViewModel> children;

    public FileNodeViewModel(Path path) {
        this(new FileNode(path));
    }

    public FileNodeViewModel(FileNode fileNode) {
        this.fileNode = fileNode;
        this.children = FXCollections.observableArrayList();
    }

    public FileNode getFileNode() {
        return fileNode;
    }

    public ObservableList<FileNodeViewModel> getChildren() {
        return children;
    }

    public String getDisplayText() {
        if (Files.isDirectory(fileNode.getPath())) {
            return String.format("%s (%s %s)", fileNode.getPath().getFileName(), fileNode.getFileCount(),
                    fileNode.getFileCount() == 1 ? Localization.lang("file") : Localization.lang("files"));
        }
        return fileNode.getPath().getFileName().toString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileNodeViewModel.class.getSimpleName() + "[", "]")
                .add("fileNode=" + fileNode)
                .add("children=" + children)
                .toString();
    }
}
