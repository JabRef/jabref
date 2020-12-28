package org.jabref.gui.externalfiles;

import java.nio.file.Path;

public class FileNodeWrapper {

    public final Path path;
    public final int fileCount;
    private boolean showIcon;
    private boolean status;
    private String message;

    public FileNodeWrapper(Path path) {
        this(path, 0);
    }

    public FileNodeWrapper(Path path, int fileCount) {
        this.path = path;
        this.fileCount = fileCount;
    }

    public void setStatusAndMessage(boolean status, String message) {
        this.status = status;
        this.message = message;
    }
    public String getMessage() {
        return this.message;

    }

}
