package org.jabref.gui.externalfiles;

import java.nio.file.Path;

public class FileNodeViewModel {

    public final Path path;
    public final int fileCount;

    public FileNodeViewModel(Path path) {
        this(path, 0);
    }

    public FileNodeViewModel(Path path, int fileCount) {
        this.path = path;
        this.fileCount = fileCount;
    }
}
