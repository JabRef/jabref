package org.jabref.gui.texparser;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.scene.control.CheckBoxTreeItem;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;

/*
 * TODO: Use Path instead of File.
 */
public class TexFileCrawler extends BackgroundTask<CheckBoxTreeItem<FileNode>> {

    private static final FileFilter TEX_FILTER = FileFilterConverter.toFileFilter(
            FileFilterConverter.toExtensionFilter(StandardFileType.TEX));

    private final Path directory;
    private int counter;

    public TexFileCrawler(Path directory) {
        this.directory = directory;
        this.counter = 0;
    }

    @Override
    protected CheckBoxTreeItem<FileNode> call() {
        return searchDirectory(directory.toFile());
    }

    private CheckBoxTreeItem<FileNode> searchDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] fileArray = directory.listFiles(TEX_FILTER);

        List<File> files = (fileArray == null) ? Collections.emptyList() : Arrays.asList(fileArray);

        CheckBoxTreeItem<FileNode> root = new CheckBoxTreeItem<>(new FileNode(directory.toPath(), 0));

        int fileCount = 0;

        fileArray = directory.listFiles(pathname -> pathname != null && pathname.isDirectory());

        List<File> subDirectories = (fileArray == null) ? Collections.emptyList() : Arrays.asList(fileArray);

        for (File subDirectory : subDirectories) {
            if (isCanceled()) {
                return root;
            }

            CheckBoxTreeItem<FileNode> subRoot = searchDirectory(subDirectory);

            if (subRoot != null && !subRoot.getChildren().isEmpty()) {
                fileCount += subRoot.getValue().getFileCount();
                root.getChildren().add(subRoot);
            }
        }

        root.setValue(new FileNode(directory.toPath(), files.size() + fileCount));

        for (File file : files) {
            root.getChildren().add(new CheckBoxTreeItem<>(new FileNode(file.toPath())));
            counter++;

            updateMessage(String.format("%s %s %s.", counter,
                    counter == 1 ? Localization.lang("file") : Localization.lang("files"),
                    Localization.lang("found")));
        }

        return root;
    }
}
