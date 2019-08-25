package org.jabref.model.metadata;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FilePreferences {
    public static final String DIR_SUFFIX = "Directory";

    private final String user;
    private final Map<Field, String> fieldFileDirectories;
    private final boolean bibLocationAsPrimary;
    private final String fileNamePattern;
    private final String fileDirPattern;

    public FilePreferences(String user,
                           Map<Field, String> fieldFileDirectories,
                           boolean bibLocationAsPrimary,
                           String fileNamePattern,
                           String fileDirPattern) {
        this.user = user;
        this.fieldFileDirectories = fieldFileDirectories;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
        this.fileNamePattern = fileNamePattern;
        this.fileDirPattern = fileDirPattern;
    }

    public String getUser() {
        return user;
    }

    public Optional<Path> getFileDirectory(Field field) {
        try {
            String value = fieldFileDirectories.get(field);
            // filter empty paths
            if ((value != null) && !value.isEmpty()) {
                Path path = Paths.get(value);
                return Optional.of(path);
            }
            return Optional.empty();
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    public Optional<Path> getFileDirectory() {
        return getFileDirectory(StandardField.FILE);
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
