package org.jabref.gui.texparser;

import java.nio.file.Path;

public class FileNode {

    private final Path path;
    private final int fileCount;

    public FileNode(Path path) {
        this(path, 0);
    }

    public FileNode(Path path, int fileCount) {
        this.path = path;
        this.fileCount = fileCount;
    }

    public Path getPath() {
        return path;
    }

    public int getFileCount() {
        return fileCount;
    }
}
