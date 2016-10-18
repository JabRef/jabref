package net.sf.jabref.logic.specialfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.Keyword;
import net.sf.jabref.model.entry.KeywordList;
import net.sf.jabref.model.entry.specialfields.SpecialField;
import net.sf.jabref.model.entry.specialfields.SpecialFields;

/**
 * @deprecated the class should be refactored and partly integrated into BibEntry
 * instead of synchronizing special fields with the keyword field, the BibEntry class should have a method
 * setSpecialField(field, newValue, syncToKeyword) which directly performs the correct action
 * i.e.sets the field to newValue(in the case syncToKeyword=false)or adds newValue to keywords(sync=true)
 */

@Deprecated
public class SpecialFieldsUtils {

    /**
     * @param e                         - Field to be handled
     * @param value                     - may be null to state that field should be emptied
     * @param be                        - BibTeXEntry to be handled
     * @param nullFieldIfValueIsTheSame - true: field is nulled if value is the same than the current value in be
     */
    public static List<FieldChange> updateField(SpecialField e, String value, BibEntry be, boolean nullFieldIfValueIsTheSame, boolean isKeywordSyncEnabled, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        UpdateField.updateField(be, e.getFieldName(), value, nullFieldIfValueIsTheSame)
                .ifPresent(fieldChange -> fieldChanges.add(fieldChange));
        // we cannot use "value" here as updateField has side effects: "nullFieldIfValueIsTheSame" nulls the field if value is the same
        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(e, be, isKeywordSyncEnabled, keywordDelimiter));

        return fieldChanges;
    }

    private static List<FieldChange> exportFieldToKeywords(SpecialField specialField, BibEntry entry, boolean isKeywordSyncEnabled, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        if (!isKeywordSyncEnabled) {
            return fieldChanges;
        }

        Optional<Keyword> newValue = entry.getField(specialField.getFieldName()).map(Keyword::new);
        KeywordList keyWords = specialField.getKeyWords();

        Optional<FieldChange> change = entry.replaceKeywords(keyWords, newValue, keywordDelimiter);
        change.ifPresent(changeValue -> fieldChanges.add(changeValue));

        return fieldChanges;
    }

    /**
     * Update keywords according to values of special fields
     */
    public static List<FieldChange> syncKeywordsFromSpecialFields(BibEntry be, boolean isKeywordSyncEnabled, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(SpecialField.PRIORITY, be, isKeywordSyncEnabled, keywordDelimiter));
        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(SpecialField.RANK, be, isKeywordSyncEnabled, keywordDelimiter));
        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(SpecialField.RELEVANCE, be, isKeywordSyncEnabled, keywordDelimiter));
        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(SpecialField.QUALITY, be, isKeywordSyncEnabled, keywordDelimiter));
        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(SpecialField.READ_STATUS, be, isKeywordSyncEnabled, keywordDelimiter));
        fieldChanges.addAll(SpecialFieldsUtils.exportFieldToKeywords(SpecialField.PRINTED, be, isKeywordSyncEnabled, keywordDelimiter));

        return fieldChanges;
    }

    private static List<FieldChange> importKeywordsForField(KeywordList keywordList, SpecialField c, BibEntry be) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        KeywordList values = c.getKeyWords();
        Optional<Keyword> newValue = Optional.empty();
        for (Keyword val : values) {
            if (keywordList.contains(val)) {
                newValue = Optional.of(val);
                break;
            }
        }

        UpdateField.updateNonDisplayableField(be, c.getFieldName(), newValue.map(Keyword::toString).orElse(null))
                .ifPresent(fieldChange -> {
                    fieldChanges.add(fieldChange);
                });
        return fieldChanges;
    }

    /**
     * updates field values according to keywords
     */
    public static List<FieldChange> syncSpecialFieldsFromKeywords(BibEntry be, Character keywordDelimiter) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        if (!be.hasField(FieldName.KEYWORDS)) {
            return fieldChanges;
        }
        KeywordList keywordList = be.getKeywords(keywordDelimiter);

        fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.PRIORITY, be));
        fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.RANK, be));
        fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.QUALITY, be));
        fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.RELEVANCE, be));
        fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.READ_STATUS, be));
        fieldChanges.addAll(SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.PRINTED, be));

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
        for(SpecialField field: SpecialField.values()){
            clone.retainAll(field.getKeyWords());
            if(!clone.isEmpty()) {
                keywordsToRemove.addAll(field.getKeyWords());
            }
        }
    }
}
