package org.jabref.logic.layout.format;

import java.util.List;

public class FileLinkPreferences {

    private final List<String> generatedDirForDatabase;
    private final List<String> fileDirForDatabase;
    public FileLinkPreferences(List<String> generatedDirForDatabase, List<String> fileDirForDatabase) {
        this.generatedDirForDatabase = generatedDirForDatabase;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public List<String> getGeneratedDirForDatabase() {
        return generatedDirForDatabase;
    }

    public List<String> getFileDirForDatabase() {
        return fileDirForDatabase;
    }
}
