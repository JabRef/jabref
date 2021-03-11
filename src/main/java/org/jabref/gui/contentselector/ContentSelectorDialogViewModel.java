package org.jabref.gui.contentselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

class ContentSelectorDialogViewModel extends AbstractViewModel {

    private static final List<Field> DEFAULT_FIELD_NAMES = Arrays.asList(StandardField.AUTHOR, StandardField.JOURNAL, StandardField.KEYWORDS, StandardField.PUBLISHER);

    private final LibraryTab libraryTab;
    private final MetaData metaData;
    private final DialogService dialogService;
    private final Map<Field, List<String>> fieldKeywordsMap = new HashMap<>();

    private ListProperty<Field> fields = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ListProperty<String> keywords = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ObjectProperty<Field> selectedField = new SimpleObjectProperty<>();
    private StringProperty selectedKeyword = new SimpleStringProperty();

    ContentSelectorDialogViewModel(LibraryTab libraryTab, DialogService dialogService) {
        this.libraryTab = libraryTab;
        this.metaData = libraryTab.getBibDatabaseContext().getMetaData();
        this.dialogService = dialogService;
        populateFieldNameKeywordsMapWithExistingValues();
        populateFieldNamesListWithValues();
    }

    private void populateFieldNamesListWithValues() {
        List<Field> existingFields = new ArrayList<>(fieldKeywordsMap.keySet());
        fields.addAll(existingFields);

        if (fields.isEmpty()) {
            DEFAULT_FIELD_NAMES.forEach(this::addFieldIfUnique);
        }
    }

    private void populateFieldNameKeywordsMapWithExistingValues() {
        metaData.getContentSelectors().getContentSelectors().forEach(
                existingContentSelector -> fieldKeywordsMap.put(existingContentSelector.getField(), new ArrayList<>(existingContentSelector.getValues()))
        );
    }

    ListProperty<Field> getFieldNamesBackingList() {
        return fields;
    }

    ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    BooleanBinding isFieldNameListEmpty() {
        return Bindings.isEmpty(fields);
    }

    BooleanBinding isNoFieldNameSelected() {
        return Bindings.isEmpty(selectedField.asString());
    }

    ListProperty<String> getKeywordsBackingList() {
        return keywords;
    }

    StringProperty selectedKeywordProperty() {
        return selectedKeyword;
    }

    BooleanBinding isNoKeywordSelected() {
        return Bindings.isEmpty(selectedKeyword);
    }

    void showInputFieldNameDialog() {
        dialogService.showInputDialogAndWait(Localization.lang("Add new field name"), Localization.lang("Field name:"))
                     .map(FieldFactory::parseField)
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

    void saveChanges() {
        List<Field> metaDataFields = metaData.getContentSelectors().getFieldsWithSelectors();
        fieldKeywordsMap.forEach((field, keywords) -> updateMetaDataContentSelector(metaDataFields, field, keywords));

        List<Field> fieldNamesToRemove = filterFieldsToRemove();
        fieldNamesToRemove.forEach(metaData::clearContentSelectors);

        libraryTab.setupMainPanel();
        libraryTab.markNonUndoableBaseChanged();
    }

    private List<Field> filterFieldsToRemove() {
        Set<Field> newlyAddedKeywords = fieldKeywordsMap.keySet();
        return metaData.getContentSelectors().getFieldsWithSelectors().stream()
                       .filter(field -> !newlyAddedKeywords.contains(field))
                       .collect(Collectors.toList());
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
