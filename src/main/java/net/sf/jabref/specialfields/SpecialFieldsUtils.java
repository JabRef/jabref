package net.sf.jabref.specialfields;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
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
     * @param e - Field to be handled
     * @param value - may be null to state that field should be emptied
     * @param be - BibTeXEntry to be handled
     * @param ce - Filled with undo info (if necessary)
     * @param nullFieldIfValueIsTheSame - true: field is nulled if value is the same than the current value in be
     */
    public static void updateField(SpecialField e, String value, BibEntry be, NamedCompound ce, boolean nullFieldIfValueIsTheSame) {
        UpdateField.updateField(be, e.getFieldName(), value, nullFieldIfValueIsTheSame)
                .ifPresent(fieldChange -> ce.addEdit(new UndoableFieldChange(fieldChange)));
        // we cannot use "value" here as updateField has side effects: "nullFieldIfValueIsTheSame" nulls the field if value is the same
        SpecialFieldsUtils.exportFieldToKeywords(e, be, ce);
    }

    private static void exportFieldToKeywords(SpecialField specialField, BibEntry entry, NamedCompound ce) {
        if (!Globals.prefs.isKeywordSyncEnabled()) {
            return;
        }

        Optional<Keyword> newValue = entry.getField(specialField.getFieldName()).map(Keyword::new);
        KeywordList keyWords = specialField.getKeyWords();

        Optional<FieldChange> change = entry.replaceKeywords(keyWords, newValue, Globals.prefs.getKeywordDelimiter());
        if (ce != null){
            change.ifPresent(changeValue -> ce.addEdit(new UndoableFieldChange(changeValue)));
        }
    }

    public static void syncKeywordsFromSpecialFields(BibEntry be) {
        syncKeywordsFromSpecialFields(be, null);
    }

    /**
     * Update keywords according to values of special fields
     *
     * @param nc indicates the undo named compound. May be null
     */
    public static void syncKeywordsFromSpecialFields(BibEntry be, NamedCompound nc) {
        SpecialFieldsUtils.exportFieldToKeywords(SpecialField.PRIORITY, be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(SpecialField.RANK, be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(SpecialField.RELEVANCE, be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(SpecialField.QUALITY, be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(SpecialField.READ_STATUS, be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(SpecialField.PRINTED, be, nc);
    }

    private static void importKeywordsForField(KeywordList keywordList, SpecialField c, BibEntry be,
            NamedCompound nc) {
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
                    if (nc != null) {
                        nc.addEdit(new UndoableFieldChange(fieldChange));
                    }
                });
    }

    public static void syncSpecialFieldsFromKeywords(BibEntry be) {
        syncSpecialFieldsFromKeywords(be, null);
    }

    /**
    * updates field values according to keywords
    *
    * @param ce indicates the undo named compound. May be null
    */
    public static void syncSpecialFieldsFromKeywords(BibEntry be, NamedCompound ce) {
        if (!be.hasField(FieldName.KEYWORDS)) {
            return;
        }
        KeywordList keywordList = be.getKeywords(Globals.prefs.getKeywordDelimiter());
        SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.PRIORITY, be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.RANK, be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.QUALITY, be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.RELEVANCE, be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.READ_STATUS, be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, SpecialField.PRINTED, be, ce);
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
