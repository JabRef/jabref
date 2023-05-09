package org.jabref.gui.preferences.entry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.BibEntryPreferences;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;

public class EntryTabViewModel implements PreferenceTabViewModel {
    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");

    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsForFieldsProperty = new SimpleStringProperty("");
    private final StringProperty nonWrappableFieldsProperty = new SimpleStringProperty("");

    private final BooleanProperty markOwnerProperty = new SimpleBooleanProperty();
    private final StringProperty markOwnerNameProperty = new SimpleStringProperty("");
    private final BooleanProperty markOwnerOverwriteProperty = new SimpleBooleanProperty();
    private final BooleanProperty addCreationDateProperty = new SimpleBooleanProperty();
    private final BooleanProperty addModificationDateProperty = new SimpleBooleanProperty();

    private final FieldPreferences fieldPreferences;
    private final BibEntryPreferences bibEntryPreferences;
    private final OwnerPreferences ownerPreferences;
    private final TimestampPreferences timestampPreferences;
    private final GeneralPreferences generalPreferences;

    public EntryTabViewModel(PreferencesService preferencesService) {
        this.bibEntryPreferences = preferencesService.getBibEntryPreferences();
        this.fieldPreferences = preferencesService.getFieldPreferences();
        this.ownerPreferences = preferencesService.getOwnerPreferences();
        this.timestampPreferences = preferencesService.getTimestampPreferences();
        this.generalPreferences = preferencesService.getGeneralPreferences();
    }

    @Override
    public void setValues() {
        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(generalPreferences.getDefaultBibDatabaseMode());

        keywordSeparatorProperty.setValue(bibEntryPreferences.getKeywordSeparator().toString());

        resolveStringsProperty.setValue(fieldPreferences.shouldResolveStrings());
        resolveStringsForFieldsProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getResolvableFields()));
        nonWrappableFieldsProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getNonWrappableFields()));

        markOwnerProperty.setValue(ownerPreferences.isUseOwner());
        markOwnerNameProperty.setValue(ownerPreferences.getDefaultOwner());
        markOwnerOverwriteProperty.setValue(ownerPreferences.isOverwriteOwner());

        addCreationDateProperty.setValue(timestampPreferences.shouldAddCreationDate());
        addModificationDateProperty.setValue(timestampPreferences.shouldAddModificationDate());
    }

    @Override
    public void storeSettings() {
        generalPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());

        bibEntryPreferences.keywordSeparatorProperty().setValue(keywordSeparatorProperty.getValue().charAt(0));

        fieldPreferences.setResolveStrings(resolveStringsProperty.getValue());
        fieldPreferences.setResolvableFields(FieldFactory.parseFieldList(resolveStringsForFieldsProperty.getValue().trim()));
        fieldPreferences.setNonWrappableFields(FieldFactory.parseFieldList(nonWrappableFieldsProperty.getValue().trim()));

        ownerPreferences.setUseOwner(markOwnerProperty.getValue());
        ownerPreferences.setDefaultOwner(markOwnerNameProperty.getValue());
        ownerPreferences.setOverwriteOwner(markOwnerOverwriteProperty.getValue());

        timestampPreferences.setAddCreationDate(addCreationDateProperty.getValue());
        timestampPreferences.setAddModificationDate(addModificationDateProperty.getValue());
    }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() {
        return this.bibliographyModeListProperty;
    }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() {
        return this.selectedBiblatexModeProperty;
    }

    public StringProperty keywordSeparatorProperty() {
        return keywordSeparatorProperty;
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStringsProperty;
    }

    public StringProperty resolveStringsForFieldsProperty() {
        return resolveStringsForFieldsProperty;
    }

    public StringProperty nonWrappableFieldsProperty() {
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
}
