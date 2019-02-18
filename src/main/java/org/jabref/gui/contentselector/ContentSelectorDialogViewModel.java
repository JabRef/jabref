package org.jabref.gui.contentselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import static com.google.common.collect.ImmutableList.of;

class ContentSelectorDialogViewModel extends AbstractViewModel {

    private static final List<String> DEFAULT_FIELD_NAMES = of(FieldName.AUTHOR, FieldName.JOURNAL, FieldName.KEYWORDS, FieldName.PUBLISHER);

    private final BasePanel basePanel;
    private final MetaData metaData;
    private final DialogService dialogService;
    private final Map<String, List<String>> fieldNameKeywordsMap = new HashMap<>();

    private ListProperty<String> fieldNames = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ListProperty<String> keywords = new SimpleListProperty<>(FXCollections.observableArrayList());
    private StringProperty selectedFieldName = new SimpleStringProperty();
    private StringProperty selectedKeyword = new SimpleStringProperty();

    ContentSelectorDialogViewModel(BasePanel basePanel, DialogService dialogService) {
        this.basePanel = basePanel;
        this.metaData = basePanel.getBibDatabaseContext().getMetaData();
        this.dialogService = dialogService;
        populateFieldNameKeywordsMapWithExistingValues();
        populateFieldNamesListWithValues();
    }

    private void populateFieldNamesListWithValues() {
        List<String> existingFieldNames = new ArrayList<>(fieldNameKeywordsMap.keySet());
        Collections.sort(existingFieldNames);
        fieldNames.addAll(existingFieldNames);

        if (fieldNames.isEmpty()) {
            DEFAULT_FIELD_NAMES.forEach(this::addFieldNameIfUnique);
        }
    }

    private void populateFieldNameKeywordsMapWithExistingValues() {
        metaData.getContentSelectors().getContentSelectors().forEach(
                existingContentSelector -> fieldNameKeywordsMap.put(existingContentSelector.getFieldName(), new ArrayList<>(existingContentSelector.getValues()))
        );
    }

    ListProperty<String> getFieldNamesBackingList() {
        return fieldNames;
    }

    StringProperty selectedFieldNameProperty() {
        return selectedFieldName;
    }

    BooleanBinding isFieldNameListEmpty() {
        return Bindings.isEmpty(fieldNames);
    }

    BooleanBinding isNoFieldNameSelected() {
        return Bindings.isEmpty(selectedFieldName);
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
                .ifPresent(this::addFieldNameIfUnique);
    }

    private void addFieldNameIfUnique(String fieldNameToAdd) {
        boolean exists = fieldNameKeywordsMap.containsKey(fieldNameToAdd);
        if (exists) {
            dialogService.showErrorDialogAndWait(Localization.lang("Field name \"%0\" already exists", fieldNameToAdd));
            return;
        }

        fieldNameKeywordsMap.put(fieldNameToAdd, new ArrayList<>());
        fieldNames.add(fieldNameToAdd);
    }

    void showRemoveFieldNameConfirmationDialog(String fieldNameToRemove) {
        if (fieldNameToRemove == null) {
            dialogService.showErrorDialogAndWait(Localization.lang("No field name selected!"));
            return;
        }

        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove field name"),
                Localization.lang("Are you sure you want to remove field name: \"%0\"?", fieldNameToRemove)
        );

        if (deleteConfirmed) {
            removeFieldName(fieldNameToRemove);
        }
    }

    private void removeFieldName(String fieldNameToRemove) {
        fieldNameKeywordsMap.remove(fieldNameToRemove);
        fieldNames.remove(fieldNameToRemove);
    }

    void populateKeywords(String selectedFieldName) {
        keywords.clear();
        if (selectedFieldName != null) {
            keywords.addAll(fieldNameKeywordsMap.get(selectedFieldName));
        }
    }

    void showInputKeywordDialog(String selectedFieldName) {
        dialogService.showInputDialogAndWait(Localization.lang("Add new keyword"), Localization.lang("Keyword:"))
                .ifPresent(newKeyword -> addKeywordIfUnique(selectedFieldName, newKeyword));
    }

    private void addKeywordIfUnique(String fieldName, String keywordToAdd) {
        boolean exists = fieldNameKeywordsMap.get(fieldName).contains(keywordToAdd);
        if (exists) {
            dialogService.showErrorDialogAndWait(Localization.lang("Keyword \"%0\" already exists", keywordToAdd));
            return;
        }

        List<String> existingKeywords = fieldNameKeywordsMap.getOrDefault(fieldName, new ArrayList<>());
        existingKeywords.add(keywordToAdd);
        fieldNameKeywordsMap.put(fieldName, existingKeywords);
        keywords.add(keywordToAdd);
        populateKeywords(fieldName);
    }

    void showRemoveKeywordConfirmationDialog(String fieldName, String keywordToRemove) {
        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait(Localization.lang("Remove keyword"), Localization.lang("Are you sure you want to remove keyword: \"%0\"?", keywordToRemove));
        if (deleteConfirmed) {
            removeKeyword(fieldName, keywordToRemove);
        }
    }

    private void removeKeyword(String fieldName, String keywordToRemove) {
        fieldNameKeywordsMap.get(fieldName).remove(keywordToRemove);
        keywords.remove(keywordToRemove);
    }

    void saveChanges() {
        List<String> metaDataFieldNames = metaData.getContentSelectors().getFieldNamesWithSelectors();
        fieldNameKeywordsMap.forEach((fieldName, keywords) -> updateMetaDataContentSelector(metaDataFieldNames, fieldName, keywords));

        List<String> fieldNamesToRemove = filterFieldNamesToRemove();
        fieldNamesToRemove.forEach(metaData::clearContentSelectors);

        basePanel.setupMainPanel();
        basePanel.markNonUndoableBaseChanged();
    }

    private List<String> filterFieldNamesToRemove() {
        Set<String> newlyAddedKeywords = fieldNameKeywordsMap.keySet();
        return metaData.getContentSelectors().getFieldNamesWithSelectors().stream()
                .filter(fieldName -> !newlyAddedKeywords.contains(fieldName))
                .collect(Collectors.toList());
    }

    private void updateMetaDataContentSelector(List<String> existingFieldNames, String fieldName, List<String> keywords) {
        boolean fieldNameDoNotExists = !existingFieldNames.contains(fieldName);
        if (fieldNameDoNotExists) {
            metaData.addContentSelector(new ContentSelector(fieldName, keywords));
        }

        if (keywordsHaveChanged(fieldName, keywords)) {
            metaData.clearContentSelectors(fieldName);
            metaData.addContentSelector(new ContentSelector(fieldName, keywords));
        }
    }

    private boolean keywordsHaveChanged(String fieldName, List<String> keywords) {
        HashSet<String> keywordsSet = asHashSet(keywords);
        List<String> existingKeywords = metaData.getContentSelectorValuesForField(fieldName);
        if (!keywordsSet.equals(asHashSet(existingKeywords))) {
            return true;
        }
        return !keywordsSet.isEmpty() && existingKeywords.isEmpty();
    }

    private HashSet<String> asHashSet(List<String> listToConvert) {
        return new HashSet<>(listToConvert);
    }
}
