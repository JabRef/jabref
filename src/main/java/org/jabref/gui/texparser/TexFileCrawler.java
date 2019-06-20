package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.CheckBoxTreeItem;

import org.jabref.gui.util.BackgroundTask;

public class TexFileCrawler extends BackgroundTask<CheckBoxTreeItem<FileNode>> {

    private static final String TEX_EXT = ".tex";

    private final Path directory;
    private int count;

    public TexFileCrawler(Path directory) {
        this.directory = directory;
        this.count = 0;
    }

    @Override
    protected CheckBoxTreeItem<FileNode> call() throws IOException {
        return searchDirectory(directory);
    }

    private CheckBoxTreeItem<FileNode> searchDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return null;
        }

        CheckBoxTreeItem<FileNode> parent = new CheckBoxTreeItem<>(new FileNode(directory));
        int fileCount = 0;

        List<Path> files = Files.list(directory)
                                .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(TEX_EXT))
                                .collect(Collectors.toList());

        List<Path> subDirectories = Files.list(directory)
                                         .filter(path -> Files.isDirectory(path))
                                         .collect(Collectors.toList());

        for (Path subDirectory : subDirectories) {
            if (isCanceled()) {
                return parent;
            }

            CheckBoxTreeItem<FileNode> subRoot = searchDirectory(subDirectory);

            if (subRoot != null && !subRoot.getChildren().isEmpty()) {
                fileCount += subRoot.getValue().getFileCount();
                parent.getChildren().add(subRoot);
            }
        }

        parent.setValue(new FileNode(directory, files.size() + fileCount));

        for (Path file : files) {
            parent.getChildren().add(new CheckBoxTreeItem<>(new FileNode(file)));
            count++;
        }

        return parent;
    }
}
