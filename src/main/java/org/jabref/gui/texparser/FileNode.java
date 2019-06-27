package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

import org.jabref.logic.l10n.Localization;

public class FileNode {

    private final Path path;
    private int fileCount;

    public FileNode(Path path) {
        this.path = path;
        this.fileCount = 0;
    }

    public Path getPath() {
        return path;
    }

    public void increaseFileCount() {
        this.fileCount++;
    }

    public String getDisplayText() {
        if (Files.isDirectory(path)) {
            return String.format("%s (%s %s)", path.getFileName(), fileCount,
                    fileCount == 1 ? Localization.lang("file") : Localization.lang("files"));
        }

        return path.getFileName().toString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileNode.class.getSimpleName() + "[", "]")
                .add("path=" + path)
                .add("fileCount=" + fileCount)
                .toString();
    }
}
