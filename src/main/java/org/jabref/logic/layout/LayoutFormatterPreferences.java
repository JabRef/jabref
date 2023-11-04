package org.jabref.logic.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.StringProperty;

import org.jabref.logic.layout.format.NameFormatterPreferences;

public class LayoutFormatterPreferences {

    private final NameFormatterPreferences nameFormatterPreferences;
    private final StringProperty mainFileDirectoryProperty;
    private final Map<String, String> customExportNameFormatters = new HashMap<>();

    public LayoutFormatterPreferences(NameFormatterPreferences nameFormatterPreferences,
                                      StringProperty mainFileDirectoryProperty) {
        this.nameFormatterPreferences = nameFormatterPreferences;
        this.mainFileDirectoryProperty = mainFileDirectoryProperty;
    }

    public NameFormatterPreferences getNameFormatterPreferences() {
        return nameFormatterPreferences;
    }

    public String getMainFileDirectory() {
        return mainFileDirectoryProperty.get();
    }

    public Optional<String> getCustomExportNameFormatter(String formatterName) {
        return Optional.ofNullable(customExportNameFormatters.get(formatterName));
    }

    public void clearCustomExportNameFormatters() {
        customExportNameFormatters.clear();
    }

    public void putCustomExportNameFormatter(String formatterName, String contents) {
        customExportNameFormatters.put(formatterName, contents);
    }
}
