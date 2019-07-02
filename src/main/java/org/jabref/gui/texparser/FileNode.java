package org.jabref.gui.texparser;

import java.nio.file.Path;
import java.util.StringJoiner;

public class FileNode {

    private Path path;
    private int fileCount;

    public FileNode(Path path, int fileCount) {
        this(path);
        this.fileCount = fileCount;
    }

    public FileNode(Path path) {
        this.path = path;
        this.fileCount = 0;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileNode.class.getSimpleName() + "[", "]")
                .add("path=" + path)
                .add("fileCount=" + fileCount)
                .toString();
    }
}
