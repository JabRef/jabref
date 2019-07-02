package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.util.BackgroundTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkFileTreeTask extends BackgroundTask<FileNodeViewModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalkFileTreeTask.class);
    private static final String TEX_EXT = ".tex";

    private final Path directory;

    public WalkFileTreeTask(Path directory) {
        this.directory = directory;
    }

    @Override
    protected FileNodeViewModel call() {
        return searchDirectory(directory);
    }

    private FileNodeViewModel searchDirectory(Path directory) {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return null;
        }

        FileNodeViewModel parent = new FileNodeViewModel(directory);
        int fileCount = 0;

        List<Path> files = null;
        List<Path> subDirectories = null;

        try {
            files = Files.list(directory)
                         .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(TEX_EXT))
                         .collect(Collectors.toList());

            subDirectories = Files.list(directory)
                                  .filter(path -> Files.isDirectory(path))
                                  .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Problem scanning files and directories.", e);
            return null;
        }

        for (Path subDirectory : subDirectories) {
            if (isCanceled()) {
                return parent;
            }

            FileNodeViewModel subRoot = searchDirectory(subDirectory);

            if (subRoot != null && !subRoot.getChildren().isEmpty()) {
                fileCount += subRoot.getFileNode().getFileCount();
                parent.getChildren().add(subRoot);
            }
        }

        parent.getFileNode().setFileCount(files.size() + fileCount);

        files.forEach(file -> parent.getChildren().add(new FileNodeViewModel(file)));

        return parent;
    }
}
