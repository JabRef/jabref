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
import net.sf.jabref.model.entry.SpecialFields;

@Deprecated // the class should be refactored and partly integrated into BibEntry
// instead of synchronizing special fields with the keyword field, the BibEntry class should have a method
// setSpecialField(field, newValue, syncToKeyword) which directly performs the correct action
// i.e. sets the field to newValue (in the case syncToKeyword = false) or adds newValue to keywords (sync = true)
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

    /**
     * @param fieldName the fieldName
     * @return an instance of that field. The returned object is a singleton. null is returned if fieldName does not indicate a special field
     */
    public static Optional<SpecialField> getSpecialFieldInstanceFromFieldName(String fieldName) {
        switch (fieldName) {
            case SpecialFields.FIELDNAME_PRIORITY:
                return Optional.of(SpecialField.PRIORITY);
            case SpecialFields.FIELDNAME_QUALITY:
                return Optional.of(SpecialField.QUALITY);
            case SpecialFields.FIELDNAME_RANKING:
                return Optional.of(SpecialField.RANK);
            case SpecialFields.FIELDNAME_RELEVANCE:
                return Optional.of(SpecialField.RELEVANCE);
            case SpecialFields.FIELDNAME_READ:
                return Optional.of(SpecialField.READ_STATUS);
            case SpecialFields.FIELDNAME_PRINTED:
                return Optional.of(SpecialField.PRINTED);
            default:
                return Optional.empty();
        }
    }

    /**
     * @param fieldName the name of the field to check
     * @return true if given field is a special field, false otherwise
     */
    public static boolean isSpecialField(String fieldName) {
        return SpecialFieldsUtils.getSpecialFieldInstanceFromFieldName(fieldName).isPresent();
    }
}
