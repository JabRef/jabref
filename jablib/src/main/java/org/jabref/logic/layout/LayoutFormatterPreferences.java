package org.jabref.logic.layout;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.preferences.DOIPreferences;

public class LayoutFormatterPreferences {

    private final NameFormatterPreferences nameFormatterPreferences;

    private final DOIPreferences doiPreferences;
    private final ReadOnlyObjectProperty<Path> mainFileDirectoryProperty;
    private final Map<String, String> customExportNameFormatters = new HashMap<>();

    public LayoutFormatterPreferences(NameFormatterPreferences nameFormatterPreferences,
                                      DOIPreferences doiPreferences,
                                      ReadOnlyObjectProperty<Path> mainFileDirectoryProperty) {
        this.nameFormatterPreferences = nameFormatterPreferences;
        this.mainFileDirectoryProperty = mainFileDirectoryProperty;
        this.doiPreferences = doiPreferences;
    }

    /// Temporary dummy for PreviewPreferences
    public static LayoutFormatterPreferences getDefault() {
        return new LayoutFormatterPreferences(
                new NameFormatterPreferences(List.of(), List.of()),
                new DOIPreferences(false, ""),
                new SimpleObjectProperty<>(Path.of("")));
    }

    public NameFormatterPreferences getNameFormatterPreferences() {
        return nameFormatterPreferences;
    }

    public Path getMainFileDirectory() {
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

    public DOIPreferences getDoiPreferences() {
        return doiPreferences;
    }
}
