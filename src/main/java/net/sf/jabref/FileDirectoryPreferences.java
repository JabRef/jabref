package net.sf.jabref;

import java.util.Optional;

public class FileDirectoryPreferences {
    private final String user;
    private final String fieldDirectory;
    private final boolean bibLocationAsPrimary;
    private final String fieldName;

    public FileDirectoryPreferences(String fieldName, String user, String fieldDirectory, boolean bibLocationAsPrimary) {
        this.fieldName = fieldName;
        this.user = user;
        this.fieldDirectory = fieldDirectory;
        this.bibLocationAsPrimary = bibLocationAsPrimary;
    }

    public String getUser() {
        return user;
    }

    public Optional<String> getFieldDirectory() {
        return Optional.ofNullable(fieldDirectory);
    }

    public boolean isBibLocationAsPrimary() {
        return bibLocationAsPrimary;
    }

    public String getFieldName() {
        return fieldName;
    }
}
