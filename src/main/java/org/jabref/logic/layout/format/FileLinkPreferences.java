package org.jabref.logic.layout.format;

import java.nio.file.Path;
import java.util.List;

public class FileLinkPreferences {

    private final String mainFileDirectory;
    private final List<Path> fileDirForDatabase;

    public FileLinkPreferences(String mainFileDirectory, List<Path> fileDirForDatabase) {
        this.mainFileDirectory = mainFileDirectory;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public String getMainFileDirectory() {
        return mainFileDirectory;
    }

    public List<Path> getFileDirForDatabase() {
        return fileDirForDatabase;
    }
}
