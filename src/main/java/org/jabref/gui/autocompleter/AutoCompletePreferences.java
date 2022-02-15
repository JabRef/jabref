package org.jabref.gui.autocompleter;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

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

    public String getCompleteNamesAsString() {
        return FieldFactory.serializeFieldsList(completeFields);
    }
}
