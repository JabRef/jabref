package org.jabref.preferences;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class FilePreferences {

    public static final String[] DEFAULT_FILENAME_PATTERNS = new String[] {"[bibtexkey]", "[bibtexkey] - [title]"};

    private final String user;
    private final String mainFileDirectory;
    private final boolean bibLocationAsPrimary;
    private final String fileNamePattern;
    private final String fileDirPattern;
    private boolean shouldDownloadLinkedFiles;
    private final boolean shouldSearchFilesOnOpen;
    private final boolean shouldOpenBrowseOnCreate;

    public FilePreferences(String user,
                           String mainFileDirectory,
                           boolean bibLocationAsPrimary,
                           String fileNamePattern,
                           String fileDirPattern,
                           boolean shouldDownloadLinkedFiles,
                           boolean shouldSearchFilesOnOpen,
                           boolean shouldOpenBrowseOnCreate) {
        this.user = user;
        this.mainFileDirectory = mainFileDirectory;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
        this.fileNamePattern = fileNamePattern;
        this.fileDirPattern = fileDirPattern;
        this.shouldDownloadLinkedFiles = shouldDownloadLinkedFiles;
        this.shouldSearchFilesOnOpen = shouldSearchFilesOnOpen;
        this.shouldOpenBrowseOnCreate = shouldOpenBrowseOnCreate;
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

    public String getFileDirectoryPattern() {
        return fileDirPattern;
    }

    public boolean shouldDownloadLinkedFiles() {
        return shouldDownloadLinkedFiles;
    }

    public FilePreferences withShouldDownloadLinkedFiles(boolean newShouldDownloadLinkedFiles) {
        this.shouldDownloadLinkedFiles = newShouldDownloadLinkedFiles;
        return this;
    }

    public boolean shouldSearchFilesOnOpen() {
        return shouldSearchFilesOnOpen;
    }

    public boolean shouldOpenBrowseOnCreate() {
        return shouldOpenBrowseOnCreate;
    }
}
