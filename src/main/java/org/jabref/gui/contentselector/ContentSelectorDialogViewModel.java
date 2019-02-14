package org.jabref.gui.contentselector;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jabref.gui.DialogService;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.MetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;

class ContentSelectorDialogViewModel {

    private static final List<String> DEFAULT_FIELD_NAMES = of(FieldName.AUTHOR, FieldName.JOURNAL, FieldName.KEYWORDS, FieldName.PUBLISHER);

    private final MetaData metaData;
    private final DialogService dialogService;
    private final Map<String, List<String>> fieldNameKeywordsMap = new HashMap<>();

    private ObservableList<String> fieldNames = FXCollections.observableArrayList();
    private ObservableList<String> keywords = FXCollections.observableArrayList();

    ContentSelectorDialogViewModel(MetaData metaData, DialogService dialogService) {
        this.metaData = metaData;
        this.dialogService = dialogService;
        initFieldNameKeywordsMapWithExistingValues();

    }

    private void initFieldNameKeywordsMapWithExistingValues() {
        metaData.getContentSelectors().getContentSelectors().forEach(
                existingContentSelector -> fieldNameKeywordsMap.put(existingContentSelector.getFieldName(), existingContentSelector.getValues())
        );
    }

    ObservableList<String> loadFieldNames() {
        fieldNames.addAll(fieldNameKeywordsMap.keySet());

        if (fieldNames.isEmpty()) {
            DEFAULT_FIELD_NAMES.forEach(this::addFieldNameIfUnique);
        }
        return fieldNames;
    }

    ObservableList<String> getKeywordsBackingList() {
        return keywords;
    }

    private void addFieldNameIfUnique(String fieldNameToAdd) {
        boolean exists = fieldNameKeywordsMap.containsKey(fieldNameToAdd);
        if (exists) {
            dialogService.showErrorDialogAndWait(format("Field name \"%s\" already exists", fieldNameToAdd));
            return;
        }

        fieldNameKeywordsMap.put(fieldNameToAdd, new ArrayList<>());
        fieldNames.add(fieldNameToAdd);
    }

    private void removeFieldName(String fieldNameToRemove) {
        fieldNameKeywordsMap.remove(fieldNameToRemove);
        fieldNames.remove(fieldNameToRemove);
    }

    void showInputFieldNameDialog() {
        dialogService.showInputDialogAndWait("Add new field name", "Field name:")
                .ifPresent(this::addFieldNameIfUnique);
    }

    void showRemoveFieldNameConfirmationDialog(String fieldNameToRemove) {
        if (fieldNameToRemove == null) {
            dialogService.showErrorDialogAndWait("No field name selected!");
            return;
        }

        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait("Remove field name", "Are you sure you want to remove field name: \"" + fieldNameToRemove + "\"");
        if (deleteConfirmed) {
            removeFieldName(fieldNameToRemove);
        }
    }

    void populateKeywordsFor(String selectedFieldName) {
        keywords.clear();
        keywords.addAll(fieldNameKeywordsMap.get(selectedFieldName));
    }

    void showInputKeywordDialog(String selectedFieldName) {
        dialogService.showInputDialogAndWait("Add new field name", "Field name:")
                .ifPresent(newKeyword -> addKeyword(selectedFieldName, newKeyword));
    }

    private void addKeyword(String fieldName, String keywordToAdd) {
        boolean exists = fieldNameKeywordsMap.get(fieldName).contains(keywordToAdd);
        if (exists) {
            dialogService.showErrorDialogAndWait(format("Keyword \"%s\" already exists", keywordToAdd));
            return;
        }

        List<String> existingKeywords = fieldNameKeywordsMap.getOrDefault(fieldName, new ArrayList<>());
        existingKeywords.add(keywordToAdd);
        fieldNameKeywordsMap.put(fieldName, existingKeywords);
        keywords.add(keywordToAdd);
        populateKeywordsFor(fieldName);
    }

    void showRemoveKeywordConfirmationDialog(String fieldName, String keywordToRemove) {
        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait("Remove field name", "Are you sure you want to remove field name: \"" + keywordToRemove + "\"");
        if (deleteConfirmed) {
            removeKeyword(fieldName, keywordToRemove);
        }
    }

    private void removeKeyword(String fieldName, String keywordToRemove) {
        fieldNameKeywordsMap.get(fieldName).remove(keywordToRemove);
        keywords.remove(keywordToRemove);
    }

    boolean shouldBeRemoveKeywordButtonDisabled() {
        return keywords.isEmpty();
    }
}
