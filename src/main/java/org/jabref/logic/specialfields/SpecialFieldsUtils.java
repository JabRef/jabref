package org.jabref.logic.specialfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.UpdateField;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.specialfields.SpecialField;

/**
 * @deprecated the class should be refactored and partly integrated into BibEntry
 * instead of synchronizing special fields with the keyword field, the BibEntry class should have a method
 * setSpecialField(field, newValue, syncToKeyword) which directly performs the correct action
 * i.e.sets the field to newValue(in the case syncToKeyword=false)or adds newValue to keywords(sync=true)
 */

@Deprecated
public class SpecialFieldsUtils {

    /**
     * @param field                         - Field to be handled
     * @param value                     - may be null to state that field should be emptied
     * @param entry                        - BibTeXEntry to be handled
     * @param nullFieldIfValueIsTheSame - true: field is nulled if value is the same than the current value in be
     */
    public static List<FieldChange> updateField(SpecialField field, String value, BibEntry entry, boolean nullFieldIfValueIsTheSame, boolean isKeywordSyncEnabled, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        UpdateField.updateField(entry, field.getFieldName(), value, nullFieldIfValueIsTheSame)
                .ifPresent(fieldChange -> fieldChanges.add(fieldChange));
        // we cannot use "value" here as updateField has side effects: "nullFieldIfValueIsTheSame" nulls the field if value is the same
        if (isKeywordSyncEnabled) {
            fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(field, entry, keywordDelimiter));
        }

        return fieldChanges;
    }

    private static List<FieldChange> exportFieldToKeywords(SpecialField specialField, BibEntry entry, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        KeywordList keyWords = specialField.getKeyWords();
        Optional<Keyword> newValue = entry.getField(specialField.getFieldName()).map(Keyword::new);
        newValue.map(value -> entry.replaceKeywords(keyWords, newValue.get(), keywordDelimiter))
                .orElseGet(() -> entry.removeKeywords(keyWords, keywordDelimiter))
                .ifPresent(changeValue -> fieldChanges.add(changeValue));

        return fieldChanges;
    }

    /**
     * Update keywords according to values of special fields
     */
    public static List<FieldChange> syncKeywordsFromSpecialFields(BibEntry entry, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        for (SpecialField field : SpecialField.values()) {
            fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(field, entry, keywordDelimiter));
        }

        return fieldChanges;
    }

    private static List<FieldChange> importKeywordsForField(KeywordList keywordList, SpecialField field, BibEntry entry) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        KeywordList values = field.getKeyWords();
        Optional<Keyword> newValue = Optional.empty();
        for (Keyword keyword : values) {
            if (keywordList.contains(keyword)) {
                newValue = Optional.of(keyword);
                break;
            }
        }

        UpdateField.updateNonDisplayableField(entry, field.getFieldName(), newValue.map(Keyword::toString).orElse(null))
                .ifPresent(fieldChange -> {
                    fieldChanges.add(fieldChange);
                });
        return fieldChanges;
    }

    /**
     * Updates special field values according to keywords
     */
    public static List<FieldChange> syncSpecialFieldsFromKeywords(BibEntry entry, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        if (!entry.hasField(FieldName.KEYWORDS)) {
            return fieldChanges;
        }

        KeywordList keywordList = entry.getKeywords(keywordDelimiter);

        for (SpecialField field : SpecialField.values()) {
            fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, field, entry));
        }

        return fieldChanges;
    }

    public static void synchronizeSpecialFields(KeywordList keywordsToAdd, KeywordList keywordsToRemove) {
        // we need to check whether a special field is added
        // for each field:
        //   check if something is added
        //   if yes, add all keywords of that special fields to the keywords to be removed

        KeywordList clone;

        // Priority
        clone = keywordsToAdd.createClone();
        for (SpecialField field : SpecialField.values()) {
            clone.retainAll(field.getKeyWords());
            if (!clone.isEmpty()) {
                keywordsToRemove.addAll(field.getKeyWords());
            }
        }
    }
}
