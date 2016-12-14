package net.sf.jabref.gui.util;

import javafx.stage.FileChooser;

public class FileDialogConfiguration {

    private FileChooser.ExtensionFilter extensionFilters;

    public FileChooser.ExtensionFilter getExtensionFilters() {
        return extensionFilters;
    }

    public void setExtensionFilters(FileChooser.ExtensionFilter extensionFilters) {
        this.extensionFilters = extensionFilters;
    }

    public static class Builder {
        public Builder addExtensionFilter(String description, String... extensions) {
            new FileChooser.ExtensionFilter(description, extensions);
            return this;
        }

        public FileDialogConfiguration build() {
            return null;
        }
    }
}
