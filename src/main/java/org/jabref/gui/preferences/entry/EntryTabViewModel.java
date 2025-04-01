package org.jabref.gui.preferences.entry;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class EntryTabViewModel implements PreferenceTabViewModel {

    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");

    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();

    private final ListProperty<Field> resolvableTagsForFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> nonWrappableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty markOwnerProperty = new SimpleBooleanProperty();
    private final StringProperty markOwnerNameProperty = new SimpleStringProperty("");
    private final BooleanProperty markOwnerOverwriteProperty = new SimpleBooleanProperty();
    private final BooleanProperty addCreationDateProperty = new SimpleBooleanProperty();
    private final BooleanProperty addModificationDateProperty = new SimpleBooleanProperty();

    private final FieldPreferences fieldPreferences;
    private final BibEntryPreferences bibEntryPreferences;
    private final OwnerPreferences ownerPreferences;
    private final TimestampPreferences timestampPreferences;

    public EntryTabViewModel(CliPreferences preferences) {
        this.bibEntryPreferences = preferences.getBibEntryPreferences();
        this.fieldPreferences = preferences.getFieldPreferences();
        this.ownerPreferences = preferences.getOwnerPreferences();
        this.timestampPreferences = preferences.getTimestampPreferences();
    }

    @Override
    public void setValues() {
        keywordSeparatorProperty.setValue(bibEntryPreferences.getKeywordSeparator().toString());

        resolveStringsProperty.setValue(fieldPreferences.shouldResolveStrings());
        resolvableTagsForFieldsProperty.setValue(FXCollections.observableArrayList(fieldPreferences.getResolvableFields()));
        nonWrappableFieldsProperty.setValue(FXCollections.observableArrayList(fieldPreferences.getNonWrappableFields()));

        markOwnerProperty.setValue(ownerPreferences.isUseOwner());
        markOwnerNameProperty.setValue(ownerPreferences.getDefaultOwner());
        markOwnerOverwriteProperty.setValue(ownerPreferences.isOverwriteOwner());

        addCreationDateProperty.setValue(timestampPreferences.shouldAddCreationDate());
        addModificationDateProperty.setValue(timestampPreferences.shouldAddModificationDate());
    }

    @Override
    public void storeSettings() {
        bibEntryPreferences.keywordSeparatorProperty().setValue(keywordSeparatorProperty.getValue().charAt(0));

        fieldPreferences.setResolveStrings(resolveStringsProperty.getValue());
        fieldPreferences.setResolvableFields(resolvableTagsForFieldsProperty.getValue());
        fieldPreferences.setNonWrappableFields(resolvableTagsForFieldsProperty.getValue());

        ownerPreferences.setUseOwner(markOwnerProperty.getValue());
        ownerPreferences.setDefaultOwner(markOwnerNameProperty.getValue());
        ownerPreferences.setOverwriteOwner(markOwnerOverwriteProperty.getValue());

        timestampPreferences.setAddCreationDate(addCreationDateProperty.getValue());
        timestampPreferences.setAddModificationDate(addModificationDateProperty.getValue());
    }

    public StringProperty keywordSeparatorProperty() {
        return keywordSeparatorProperty;
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStringsProperty;
    }

    public ListProperty<Field> resolvableTagsForFieldsProperty() {
        return resolvableTagsForFieldsProperty;
    }

    public ListProperty<Field> nonWrappableFieldsProperty() {
        return nonWrappableFieldsProperty;
    }

    // Entry owner
    public BooleanProperty markOwnerProperty() {
        return this.markOwnerProperty;
    }

    public StringProperty markOwnerNameProperty() {
        return this.markOwnerNameProperty;
    }

    public BooleanProperty markOwnerOverwriteProperty() {
        return this.markOwnerOverwriteProperty;
    }

    // Time stamp

    public BooleanProperty addCreationDateProperty() {
        return addCreationDateProperty;
    }

    public BooleanProperty addModificationDateProperty() {
        return addModificationDateProperty;
    }

    public StringConverter<Field> getFieldStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field.getDisplayName();
            }

            @Override
            public Field fromString(String string) {
                return FieldFactory.parseField(string);
            }
        };
    }

    public List<Field> getSuggestions(String request) {
        return FieldFactory.getAllFieldsWithOutInternal().stream()
                           .filter(field -> field.getDisplayName().toLowerCase().contains(request.toLowerCase()))
                           .collect(Collectors.toList());
    }
}
