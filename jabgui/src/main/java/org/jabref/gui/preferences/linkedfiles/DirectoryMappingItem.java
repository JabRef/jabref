package org.jabref.gui.preferences.linkedfiles;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DirectoryMappingItem {
    private final StringProperty directory;
    private final StringProperty mappedDirectory;

    public DirectoryMappingItem(String directory, String mappedDirectory) {
        this.directory = new SimpleStringProperty(directory);
        this.mappedDirectory = new SimpleStringProperty(mappedDirectory);
    }

    public StringProperty directoryProperty() {
        return directory;
    }

    public StringProperty mappedDirectoryProperty() {
        return mappedDirectory;
    }

    public String getDirectory() {
        return directory.get();
    }

    public String getMappedDirectory() {
        return mappedDirectory.get();
    }
}
