package org.jabref.gui.externalfiles;

import java.nio.file.Path;

public class FileNodeWrapper {

    public final Path path;
    public final int fileCount;

    public FileNodeWrapper(Path path) {
        this(path, 0);
    }

    public FileNodeWrapper(Path path, int fileCount) {
        this.path = path;
        this.fileCount = fileCount;
    }
}
