package org.jabref.model.metadata;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.FieldName;

public class FileDirectoryPreferences {
    public static final String DIR_SUFFIX = "Directory";

    private final String user;
    private final Map<String, String> fieldFileDirectories;
    private final boolean bibLocationAsPrimary;


    public FileDirectoryPreferences(String user, Map<String, String> fieldFileDirectories, boolean bibLocationAsPrimary) {
        this.user = user;
        this.fieldFileDirectories = fieldFileDirectories;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
    }

    public String getUser() {
        return user;
    }

    public Optional<Path> getFileDirectory(String field) {
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
        return getFileDirectory(FieldName.FILE);
    }

    public boolean isBibLocationAsPrimary() {
        return bibLocationAsPrimary;
    }
}
