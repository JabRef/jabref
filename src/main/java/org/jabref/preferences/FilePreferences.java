package org.jabref.preferences;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class FilePreferences {

    public static final String[] DEFAULT_FILENAME_PATTERNS = new String[] {"[bibtexkey]", "[bibtexkey] - [title]"};

    private final String user;
    private final String mainFileDirectory;
    private final boolean shouldStoreFilesRelativeToBibFile;
    private final String fileNamePattern;
    private final String fileDirPattern;
    private boolean shouldDownloadLinkedFiles;

    public FilePreferences(String user,
                           String mainFileDirectory,
                           boolean shouldStoreFilesRelativeToBibFile,
                           String fileNamePattern,
                           String fileDirPattern,
                           boolean shouldDownloadLinkedFiles) {
        this.user = user;
        this.mainFileDirectory = mainFileDirectory;
        this.shouldStoreFilesRelativeToBibFile = shouldStoreFilesRelativeToBibFile;
        this.fileNamePattern = fileNamePattern;
        this.fileDirPattern = fileDirPattern;
        this.shouldDownloadLinkedFiles = shouldDownloadLinkedFiles;
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

    public boolean shouldStoreFilesRelativeToBib() {
        return shouldStoreFilesRelativeToBibFile;
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
}
