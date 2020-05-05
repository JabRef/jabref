package org.jabref.model.metadata;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class FilePreferences {

    private final String user;
    private final String mainFileDirectory;
    private final boolean bibLocationAsPrimary;
    private final String fileNamePattern;
    private final String fileDirPattern;
    private final boolean downloadLinkedFiles;

    public FilePreferences(String user,
                           String mainFileDirectory,
                           boolean bibLocationAsPrimary,
                           String fileNamePattern,
                           String fileDirPattern,
                           boolean downloadLinkedFiles) {
        this.user = user;
        this.mainFileDirectory = mainFileDirectory;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
        this.fileNamePattern = fileNamePattern;
        this.fileDirPattern = fileDirPattern;
        this.downloadLinkedFiles = downloadLinkedFiles;
    }

    public String getUser() {
        return user;
    }

    public Optional<Path> getFileDirectory() {
        if (StringUtil.isBlank(mainFileDirectory)) {
            return Optional.empty();
        } else {
            return Optional.of(Path.of(mainFileDirectory));
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

    public boolean getDownloadLinkedFiles() { return downloadLinkedFiles; }
}
