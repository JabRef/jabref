package org.jabref.logic.layout.format;

import java.util.List;

public class FileLinkPreferences {

    private final String mainFileDirectory;
    private final List<String> fileDirForDatabase;

    public FileLinkPreferences(String mainFileDirectory, List<String> fileDirForDatabase) {
        this.mainFileDirectory = mainFileDirectory;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public String getMainFileDirectory() {
        return mainFileDirectory;
    }

    public List<String> getFileDirForDatabase() {
        return fileDirForDatabase;
    }
}
