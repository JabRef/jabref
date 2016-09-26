package net.sf.jabref.model.metadata;

import java.util.Map;
import java.util.Optional;

import net.sf.jabref.model.entry.FieldName;

public class FileDirectoryPreferences {
    private final String user;
    private final Map<String, String> fieldFileDirectories;
    private final boolean bibLocationAsPrimary;
    public static final String DIR_SUFFIX = "Directory";


    public FileDirectoryPreferences(String user, Map<String, String> fieldFileDirectories,
            boolean bibLocationAsPrimary) {
        this.user = user;
        this.fieldFileDirectories = fieldFileDirectories;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
    }

    public String getUser() {
        return user;
    }

    public Optional<String> getFileDirectory(String field) {
        return Optional.ofNullable(fieldFileDirectories.get(field));
    }

    public Optional<String> getFileDirectory() {
        return getFileDirectory(FieldName.FILE);
    }

    public boolean isBibLocationAsPrimary() {
        return bibLocationAsPrimary;
    }
}
