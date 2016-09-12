package net.sf.jabref.logic.specialfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.SpecialFields;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;


public class SpecialFieldsUtils {


    /****************************************************/
    /** generic treatment                              **/
    /** no special treatment any more, thanks to enums **/
    /****************************************************/

    /**
     * @param field - Field to be handled
     * @param value - may be null to state that field should be emptied
     * @param entry - BibTeXEntry to be handled
     * @param changeList - Filled with undo info (if necessary)
     * @param nullFieldIfValueIsTheSame - true: field is nulled if value is the same than the current value in be
     * @param keywordSeparator - Separator for keywords
     * @param keywordSyncEnabled - true: the special field value is stored in the keywords
     */
    public static void updateField(SpecialField field, String value, BibEntry entry, List<FieldChange> changeList,
            boolean nullFieldIfValueIsTheSame, String keywordSeparator, boolean keywordSyncEnabled) {
        UpdateField.updateField(entry, field.getFieldName(), value, nullFieldIfValueIsTheSame)
                .ifPresent(changeList::add);
        if (keywordSyncEnabled) {
            // we cannot use "value" here as updateField has side effects: "nullFieldIfValueIsTheSame" nulls the field if value is the same
            SpecialFieldsUtils.exportFieldToKeywords(field, entry.getField(field.getFieldName()).orElse(null), entry, changeList,
                    keywordSeparator);
        }
    }

    private static void exportFieldToKeywords(SpecialField field, BibEntry entry, List<FieldChange> changeList,
            String keywordSeparator) {
        SpecialFieldsUtils.exportFieldToKeywords(field, entry.getField(field.getFieldName()).orElse(null), entry, changeList,
                keywordSeparator);
    }

    private static void exportFieldToKeywords(SpecialField field, String newValue, BibEntry entry,
            List<FieldChange> changeList, String keywordSeparator) {
        List<String> keywordList = new ArrayList<>(entry.getKeywords());
        List<String> specialFieldsKeywords = field.getKeyWords();

        int foundPos = -1;

        // cleanup keywords
        for (String value : specialFieldsKeywords) {
            int pos = keywordList.indexOf(value);
            if (pos >= 0) {
                foundPos = pos;
                keywordList.remove(pos);
            }
        }

        if (newValue != null) {
            if (foundPos == -1) {
                keywordList.add(newValue);
            } else {
                keywordList.add(foundPos, newValue);
            }
        }


        Optional<FieldChange> change = entry.putKeywords(keywordList, keywordSeparator);
        if (changeList != null){
            change.ifPresent(changeList::add);
        }
    }

    public static void syncKeywordsFromSpecialFields(BibEntry entry, String keywordSeparator) {
        syncKeywordsFromSpecialFields(entry, null, keywordSeparator);
    }

    /**
     * Update keywords according to values of special fields
     *
     * @param changeList indicates the undo named compound. May be null
     */
    public static void syncKeywordsFromSpecialFields(BibEntry entry, List<FieldChange> changeList, String keywordSeparator) {
        SpecialFieldsUtils.exportFieldToKeywords(Priority.getInstance(), entry, changeList, keywordSeparator);
        SpecialFieldsUtils.exportFieldToKeywords(Rank.getInstance(), entry, changeList, keywordSeparator);
        SpecialFieldsUtils.exportFieldToKeywords(Relevance.getInstance(), entry, changeList, keywordSeparator);
        SpecialFieldsUtils.exportFieldToKeywords(Quality.getInstance(), entry, changeList, keywordSeparator);
        SpecialFieldsUtils.exportFieldToKeywords(ReadStatus.getInstance(), entry, changeList, keywordSeparator);
        SpecialFieldsUtils.exportFieldToKeywords(Printed.getInstance(), entry, changeList, keywordSeparator);
    }

    private static void importKeywordsForField(Set<String> keywordList, SpecialField field, BibEntry entry,
            List<FieldChange> changeList) {
        List<String> values = field.getKeyWords();
        String newValue = null;
        for (String val : values) {
            if (keywordList.contains(val)) {
                newValue = val;
                break;
            }
        }
        Optional<FieldChange> change = UpdateField.updateNonDisplayableField(entry, field.getFieldName(), newValue);
        if (changeList != null) {
            change.ifPresent(changeList::add);
        }
    }

    public static void syncSpecialFieldsFromKeywords(BibEntry entry) {
        syncSpecialFieldsFromKeywords(entry, null);
    }

    /**
    * updates field values according to keywords
    *
    * @param changeList indicates the list of field changes. May be null
    */
    public static void syncSpecialFieldsFromKeywords(BibEntry entry, List<FieldChange> changeList) {
        if (!entry.hasField(FieldName.KEYWORDS)) {
            return;
        }
        Set<String> keywordList = EntryUtil.getSeparatedKeywords(entry);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Priority.getInstance(), entry, changeList);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Rank.getInstance(), entry, changeList);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Quality.getInstance(), entry, changeList);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Relevance.getInstance(), entry, changeList);
        SpecialFieldsUtils.importKeywordsForField(keywordList, ReadStatus.getInstance(), entry, changeList);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Printed.getInstance(), entry, changeList);
    }

    /**
     * @param fieldName the fieldName
     * @return an instance of that field. The returned object is a singleton. null is returned if fieldName does not indicate a special field
     */
    public static Optional<SpecialField> getSpecialFieldInstanceFromFieldName(String fieldName) {
        if (fieldName.equals(SpecialFields.FIELDNAME_PRIORITY)) {
            return Optional.of(Priority.getInstance());
        } else if (fieldName.equals(SpecialFields.FIELDNAME_QUALITY)) {
            return Optional.of(Quality.getInstance());
        } else if (fieldName.equals(SpecialFields.FIELDNAME_RANKING)) {
            return Optional.of(Rank.getInstance());
        } else if (fieldName.equals(SpecialFields.FIELDNAME_RELEVANCE)) {
            return Optional.of(Relevance.getInstance());
        } else if (fieldName.equals(SpecialFields.FIELDNAME_READ)) {
            return Optional.of(ReadStatus.getInstance());
        } else if (fieldName.equals(SpecialFields.FIELDNAME_PRINTED)) {
            return Optional.of(Printed.getInstance());
        } else {
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
