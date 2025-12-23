package org.jabref.gui.autocompleter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.logic.preferences.AutoCompleteFirstNameMode;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class AutoCompletePreferences {

    public enum NameFormat {
        LAST_FIRST, FIRST_LAST, BOTH
    }

    private final BooleanProperty shouldAutoComplete;
    private final ObjectProperty<AutoCompleteFirstNameMode> firstNameMode;
    private final ObjectProperty<NameFormat> nameFormat;
    private final ObservableSet<Field> completeFields;

    public AutoCompletePreferences(boolean shouldAutoComplete,
                                   AutoCompleteFirstNameMode firstNameMode,
                                   NameFormat nameFormat,
                                   Set<Field> completeFields) {
        this.shouldAutoComplete = new SimpleBooleanProperty(shouldAutoComplete);
        this.firstNameMode = new SimpleObjectProperty<>(firstNameMode);
        this.nameFormat = new SimpleObjectProperty<>(nameFormat);
        this.completeFields = FXCollections.observableSet(completeFields);
    }

    ///  Creates object with default values
    private AutoCompletePreferences() {
        this(false,                                         // Auto complete default value false
                AutoCompleteFirstNameMode.BOTH,             // Auto completer of first name uses both: full and abbreviated
                NameFormat.BOTH,                            // Name format uses both: last_first and first_last
                new LinkedHashSet<>(List.of(StandardField.AUTHOR, // Auto completer complete fields
                        StandardField.EDITOR,
                        StandardField.TITLE,
                        StandardField.JOURNAL,
                        StandardField.PUBLISHER,
                        StandardField.KEYWORDS,
                        StandardField.CROSSREF,
                        StandardField.RELATED,
                        StandardField.ENTRYSET)));
    }

    public static AutoCompletePreferences getDefault() {
        return new AutoCompletePreferences();
    }

    public void setAll(AutoCompletePreferences preferences) {
        this.shouldAutoComplete.set(preferences.shouldAutoComplete());
        this.firstNameMode.set(preferences.getFirstNameMode());
        this.nameFormat.set(preferences.getNameFormat());
        this.completeFields.addAll(preferences.getCompleteFields());
    }

    public boolean shouldAutoComplete() {
        return shouldAutoComplete.get();
    }

    public BooleanProperty autoCompleteProperty() {
        return shouldAutoComplete;
    }

    public void setAutoComplete(boolean shouldAutoComplete) {
        this.shouldAutoComplete.set(shouldAutoComplete);
    }

    /**
     * Returns how the first names are handled.
     */
    public AutoCompleteFirstNameMode getFirstNameMode() {
        return firstNameMode.get();
    }

    public ObjectProperty<AutoCompleteFirstNameMode> firstNameModeProperty() {
        return firstNameMode;
    }

    public void setFirstNameMode(AutoCompleteFirstNameMode firstNameMode) {
        this.firstNameMode.set(firstNameMode);
    }

    public NameFormat getNameFormat() {
        return nameFormat.get();
    }

    public ObjectProperty<NameFormat> nameFormatProperty() {
        return nameFormat;
    }

    public void setNameFormat(NameFormat nameFormat) {
        this.nameFormat.set(nameFormat);
    }

    /**
     * Returns the list of fields for which autocomplete is enabled
     *
     * @return List of field names
     */
    public ObservableSet<Field> getCompleteFields() {
        return completeFields;
    }
}
