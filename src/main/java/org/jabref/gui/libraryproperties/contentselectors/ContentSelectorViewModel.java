package org.jabref.gui.libraryproperties.contentselectors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.ContentSelectors;
import org.jabref.model.metadata.MetaData;

public class ContentSelectorViewModel implements PropertiesTabViewModel {

    private final MetaData metaData;

    private final DialogService dialogService;

    // The map from each field to its predefined strings ("keywords") that can be selected
    private Map<Field, List<String>> fieldKeywordsMap = new HashMap<>();

    private final ListProperty<Field> fields = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<String> keywords = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>();
    private final StringProperty selectedKeyword = new SimpleStringProperty();

    ContentSelectorViewModel(BibDatabaseContext databaseContext, DialogService dialogService) {
        this.metaData = databaseContext.getMetaData();
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        // Populate field names keyword map
        fieldKeywordsMap = ContentSelectors.getFieldKeywordsMap(metaData.getContentSelectors().getContentSelectors());

        // Populate Field names list
        List<Field> existingFields = new ArrayList<>(fieldKeywordsMap.keySet());
        fields.addAll(existingFields);

        if (fields.isEmpty()) {
            ContentSelectors.DEFAULT_FIELD_NAMES.forEach(this::addFieldIfUnique);
        }
    }

    @Override
    public void storeSettings() {
        List<Field> metaDataFields = metaData.getContentSelectors().getFieldsWithSelectors();
        List<Field> fieldNamesToRemove;

        if (ContentSelectors.isDefaultMap(fieldKeywordsMap)) {
            // Remove all fields of the content selector
            fieldNamesToRemove = metaData.getContentSelectorsSorted().stream().map(ContentSelector::getField).toList();
        } else {
            fieldNamesToRemove = determineFieldsToRemove();
        }

        fieldKeywordsMap.forEach((field, keywords) -> updateMetaDataContentSelector(metaDataFields, field, keywords));
        fieldNamesToRemove.forEach(metaData::clearContentSelectors);
    }

    public ListProperty<Field> getFieldNamesBackingList() {
        return fields;
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    public BooleanBinding isFieldNameListEmpty() {
        return Bindings.isEmpty(fields);
    }

    public BooleanBinding isNoFieldNameSelected() {
        return Bindings.isEmpty(selectedField.asString());
    }

    public ListProperty<String> getKeywordsBackingList() {
        return keywords;
    }

    StringProperty selectedKeywordProperty() {
        return selectedKeyword;
    }

    BooleanBinding isNoKeywordSelected() {
        return Bindings.isEmpty(selectedKeyword);
    }

    void showInputFieldNameDialog() {
        dialogService.showEditableChoiceDialogAndWait(Localization.lang("Add new field name"),
                             Localization.lang("Field name"),
                             Localization.lang("Add"),
                             FXCollections.observableArrayList(FieldFactory.getStandardFieldsWithCitationKey()),
                             FieldsUtil.FIELD_STRING_CONVERTER)
                     .ifPresent(this::addFieldIfUnique);
    }

    private void addFieldIfUnique(Field fieldToAdd) {
        boolean exists = fieldKeywordsMap.containsKey(fieldToAdd);
        if (exists) {
            dialogService.showErrorDialogAndWait(Localization.lang("Field name \"%0\" already exists", fieldToAdd.getDisplayName()));
            return;
        }

        fieldKeywordsMap.put(fieldToAdd, new ArrayList<>());
        fields.add(fieldToAdd);
    }

    void showRemoveFieldNameConfirmationDialog(Field fieldToRemove) {
        if (fieldToRemove == null) {
            dialogService.showErrorDialogAndWait(Localization.lang("No field name selected!"));
            return;
        }

        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove field name"),
                Localization.lang("Are you sure you want to remove field name: \"%0\"?", fieldToRemove.getDisplayName())
        );

        if (deleteConfirmed) {
            removeFieldName(fieldToRemove);
        }
    }

    private void removeFieldName(Field fieldToRemove) {
        fieldKeywordsMap.remove(fieldToRemove);
        fields.remove(fieldToRemove);
    }

    void populateKeywords(Field selectedField) {
        keywords.clear();
        if (selectedField != null) {
            keywords.addAll(fieldKeywordsMap.get(selectedField));
        }
    }

    void showInputKeywordDialog(Field selectedField) {
        dialogService.showInputDialogAndWait(Localization.lang("Add new keyword"), Localization.lang("Keyword:"))
                     .ifPresent(newKeyword -> addKeywordIfUnique(selectedField, newKeyword));
    }

    private void addKeywordIfUnique(Field field, String keywordToAdd) {
        boolean exists = fieldKeywordsMap.get(field).contains(keywordToAdd);
        if (exists) {
            dialogService.showErrorDialogAndWait(Localization.lang("Keyword \"%0\" already exists", keywordToAdd));
            return;
        }

        List<String> existingKeywords = fieldKeywordsMap.getOrDefault(field, new ArrayList<>());
        existingKeywords.add(keywordToAdd);
        existingKeywords.sort(Comparator.naturalOrder());
        fieldKeywordsMap.put(field, existingKeywords);
        populateKeywords(field);
    }

    void showRemoveKeywordConfirmationDialog(Field field, String keywordToRemove) {
        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait(Localization.lang("Remove keyword"), Localization.lang("Are you sure you want to remove keyword: \"%0\"?", keywordToRemove));
        if (deleteConfirmed) {
            removeKeyword(field, keywordToRemove);
        }
    }

    private void removeKeyword(Field field, String keywordToRemove) {
        fieldKeywordsMap.get(field).remove(keywordToRemove);
        keywords.remove(keywordToRemove);
    }

    /**
     * Determines the list of fields to remove in case a non-default map:
     *
     * <ul>
     *     <li>Fields that are not in the new list of fields and</li>
     *     <li>>all default fields that have no associated keywords</li>
     * </ul>
     */
    private List<Field> determineFieldsToRemove() {
        Set<Field> newlyAddedKeywords = fieldKeywordsMap.keySet();

        // Remove all content selectors that are not in the new list
        List<Field> result = new ArrayList<>(metaData.getContentSelectors().getFieldsWithSelectors().stream()
                                                   .filter(field -> !newlyAddedKeywords.contains(field))
                                                   .toList());
        // Remove all unset default fields
        result.addAll(fieldKeywordsMap.entrySet()
                                      .stream()
                                      .filter(entry -> ContentSelectors.DEFAULT_FIELD_NAMES.contains(entry.getKey()) && entry.getValue().isEmpty()).map(Map.Entry::getKey)
                                      .toList());

        return result;
    }

    private void updateMetaDataContentSelector(List<Field> existingFields, Field field, List<String> keywords) {
        boolean fieldNameDoNotExists = !existingFields.contains(field);
        if (fieldNameDoNotExists) {
            metaData.addContentSelector(new ContentSelector(field, keywords));
        }

        if (keywordsHaveChanged(field, keywords)) {
            metaData.clearContentSelectors(field);
            metaData.addContentSelector(new ContentSelector(field, keywords));
        }
    }

    private boolean keywordsHaveChanged(Field field, List<String> keywords) {
        HashSet<String> keywordsSet = asHashSet(keywords);
        List<String> existingKeywords = metaData.getContentSelectorValuesForField(field);
        if (!keywordsSet.equals(asHashSet(existingKeywords))) {
            return true;
        }
        return !keywordsSet.isEmpty() && existingKeywords.isEmpty();
    }

    private HashSet<String> asHashSet(List<String> listToConvert) {
        return new HashSet<>(listToConvert);
    }
}
