package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;

public class ImportTabViewModel implements PreferenceTabViewModel {

    public static final String[] DEFAULT_FILENAME_PATTERNS = new String[] { "[bibtexkey]", "[bibtexkey] - [title]" };

    private final ListProperty<String> defaultFileNamePatternsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(DEFAULT_FILENAME_PATTERNS));
    private final StringProperty fileNamePatternProperty = new SimpleStringProperty();
    private final StringProperty fileDirPatternProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public ImportTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        fileNamePatternProperty.setValue(preferences.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        fileDirPatternProperty.setValue(preferences.get(JabRefPreferences.IMPORT_FILEDIRPATTERN));
    }

    @Override
    public void storeSettings() {
        preferences.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, fileNamePatternProperty.getValue());
        preferences.put(JabRefPreferences.IMPORT_FILEDIRPATTERN, fileDirPatternProperty.getValue());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public ListProperty<String> defaultFileNamePatternsProperty() { return defaultFileNamePatternsProperty; }

    public StringProperty fileNamePatternProperty() { return fileNamePatternProperty; }

    public StringProperty fileDirPatternProperty() { return fileDirPatternProperty; }
}
