package org.jabref.model.metadata;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class FilePreferences {

    private final String user;
    private final String mainFileDirectory;
    private final boolean bibLocationAsPrimary;
    private final String fileNamePattern;
    private final String fileDirPattern;

    public FilePreferences(String user,
                           String mainFileDirectory,
                           boolean bibLocationAsPrimary,
                           String fileNamePattern,
                           String fileDirPattern) {
        this.user = user;
        this.mainFileDirectory = mainFileDirectory;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
        this.fileNamePattern = fileNamePattern;
        this.fileDirPattern = fileDirPattern;
    }

    public String getUser() {
        return user;
    }

    public Optional<Path> getFileDirectory() {
        if (StringUtil.isBlank(mainFileDirectory)) {
            return Optional.empty();
        } else {
            return Optional.of(Paths.get(mainFileDirectory));
        }
    }

    public boolean isBibLocationAsPrimary() {
        return bibLocationAsPrimary;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public String getFileDirPattern() {
        return fileDirPattern;
    }
}
