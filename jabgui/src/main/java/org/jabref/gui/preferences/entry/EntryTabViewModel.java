package org.jabref.gui.preferences.entry;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;

public class EntryTabViewModel implements PreferenceTabViewModel {

    private final StringProperty keywordSeparatorProperty = new SimpleStringProperty("");
    private final StringProperty importKeywordDelimitersProperty = new SimpleStringProperty("");
    private final ObjectProperty<BibEntryPreferences.ImportDelimiterParsingStrategy> importDelimiterParsingStrategyProperty =
            new SimpleObjectProperty<>(BibEntryPreferences.ImportDelimiterParsingStrategy.SPLIT_ON_ALL_DELIMITERS);
    private final ListProperty<BibEntryPreferences.ImportDelimiterParsingStrategy> importDelimiterParsingStrategies =
            new SimpleListProperty<>(FXCollections.observableArrayList(BibEntryPreferences.ImportDelimiterParsingStrategy.values()));

    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();

    private final ListProperty<Field> resolvableTagsFieldProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> nonWrappableTagsFieldProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty markOwnerProperty = new SimpleBooleanProperty();
    private final StringProperty markOwnerNameProperty = new SimpleStringProperty("");
    private final BooleanProperty markOwnerOverwriteProperty = new SimpleBooleanProperty();
    private final BooleanProperty addCreationDateProperty = new SimpleBooleanProperty();
    private final BooleanProperty addModificationDateProperty = new SimpleBooleanProperty();

    private final FieldPreferences fieldPreferences;
    private final BibEntryPreferences bibEntryPreferences;
    private final OwnerPreferences ownerPreferences;
    private final TimestampPreferences timestampPreferences;

    public EntryTabViewModel(BibEntryPreferences bibEntryPreferences,
                             FieldPreferences fieldPreferences,
                             OwnerPreferences ownerPreferences,
                             TimestampPreferences timestampPreferences) {
        this.bibEntryPreferences = bibEntryPreferences;
        this.fieldPreferences = fieldPreferences;
        this.ownerPreferences = ownerPreferences;
        this.timestampPreferences = timestampPreferences;
    }

    @Override
    public void setValues() {
        keywordSeparatorProperty.setValue(bibEntryPreferences.getKeywordSeparator().toString());
        importKeywordDelimitersProperty.setValue(bibEntryPreferences.getImportKeywordDelimiters());
        importDelimiterParsingStrategyProperty.setValue(bibEntryPreferences.getImportDelimiterParsingStrategy());

        resolveStringsProperty.setValue(fieldPreferences.shouldResolveStrings());
        resolvableTagsFieldProperty.setValue(FXCollections.observableArrayList(fieldPreferences.getResolvableFields()));
        nonWrappableTagsFieldProperty.setValue(FXCollections.observableArrayList(fieldPreferences.getNonWrappableFields()));

        markOwnerProperty.setValue(ownerPreferences.shouldUseOwner());
        markOwnerNameProperty.setValue(ownerPreferences.getDefaultOwner());
        markOwnerOverwriteProperty.setValue(ownerPreferences.shouldOverwriteOwner());

        addCreationDateProperty.setValue(timestampPreferences.shouldAddCreationDate());
        addModificationDateProperty.setValue(timestampPreferences.shouldAddModificationDate());
    }

    @Override
    public void storeSettings() {
        bibEntryPreferences.keywordSeparatorProperty().setValue(keywordSeparatorProperty.getValue().charAt(0));
        bibEntryPreferences.importKeywordDelimitersProperty().setValue(importKeywordDelimitersProperty.getValue());
        bibEntryPreferences.importDelimiterParsingStrategyProperty().setValue(importDelimiterParsingStrategyProperty.getValue());

        fieldPreferences.setResolveStrings(resolveStringsProperty.getValue());
        fieldPreferences.setResolvableFields(resolvableTagsFieldProperty.getValue());
        fieldPreferences.setNonWrappableFields(resolvableTagsFieldProperty.getValue());

        ownerPreferences.setUseOwner(markOwnerProperty.getValue());
        ownerPreferences.setDefaultOwner(markOwnerNameProperty.getValue());
        ownerPreferences.setOverwriteOwner(markOwnerOverwriteProperty.getValue());

        timestampPreferences.setAddCreationDate(addCreationDateProperty.getValue());
        timestampPreferences.setAddModificationDate(addModificationDateProperty.getValue());
    }

    public StringProperty keywordSeparatorProperty() {
        return keywordSeparatorProperty;
    }

    public StringProperty importKeywordDelimitersProperty() {
        return importKeywordDelimitersProperty;
    }

    public ObjectProperty<BibEntryPreferences.ImportDelimiterParsingStrategy> importDelimiterParsingStrategyProperty() {
        return importDelimiterParsingStrategyProperty;
    }

    public ListProperty<BibEntryPreferences.ImportDelimiterParsingStrategy> importDelimiterParsingStrategies() {
        return importDelimiterParsingStrategies;
    }

    public String getImportDelimiterParsingStrategyDisplayName(BibEntryPreferences.ImportDelimiterParsingStrategy parsingStrategy) {
        return switch (parsingStrategy) {
            case SPLIT_ON_ALL_DELIMITERS ->
                    Localization.lang("Split on all accepted delimiters");
            case INFER_DELIMITER_BY_PRIORITY ->
                    Localization.lang("Infer one delimiter by priority order");
        };
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStringsProperty;
    }

    public ListProperty<Field> resolvableTagsFieldProperty() {
        return resolvableTagsFieldProperty;
    }

    public ListProperty<Field> nonWrappableTagsFieldProperty() {
        return nonWrappableTagsFieldProperty;
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
                return FieldTextMapper.getDisplayName(field);
            }

            @Override
            public Field fromString(String string) {
                return FieldFactory.parseField(string);
            }
        };
    }

    public List<Field> getSuggestions(String request) {
        List<Field> suggestions = FieldFactory.getAllFieldsWithOutInternal().stream()
                                              .filter(field -> FieldTextMapper.getDisplayName(field).toLowerCase().contains(request.toLowerCase()))
                                              .collect(Collectors.toList());

        Field requestedField = FieldFactory.parseField(request.trim());
        if (!suggestions.contains(requestedField)) {
            suggestions.addFirst(requestedField);
        }

        return suggestions;
    }
}
