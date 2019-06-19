package org.jabref.gui.texparser;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.l10n.Localization;

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

    @Override
    public String toString() {
        if (Files.isDirectory(path)) {
            return String.format("%s (%s %s)", path.getFileName(), fileCount,
                    fileCount == 1 ? Localization.lang("file") : Localization.lang("files"));
        }

        return path.getFileName().toString();
    }
}
