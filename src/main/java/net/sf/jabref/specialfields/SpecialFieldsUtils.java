/*  Copyright (C) 2012-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.specialfields;

import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.entry.BibEntry;

public class SpecialFieldsUtils {

    private static final String KEYWORDS_FIELD = "keywords";
    public static final String FIELDNAME_PRIORITY = "priority";
    public static final String FIELDNAME_RANKING = "ranking";
    public static final String FIELDNAME_RELEVANCE = "relevance";
    public static final String FIELDNAME_QUALITY = "qualityassured";
    public static final String FIELDNAME_READ = "readstatus";
    public static final String FIELDNAME_PRINTED = "printed";

    public static final String PREF_SPECIALFIELDSENABLED = "specialFieldsEnabled";
    public static final Boolean PREF_SPECIALFIELDSENABLED_DEFAULT = Boolean.TRUE;

    public static final String PREF_SHOWCOLUMN_RANKING = "showRankingColumn";
    public static final Boolean PREF_SHOWCOLUMN_RANKING_DEFAULT = Boolean.TRUE;

    public static final String PREF_SHOWCOLUMN_PRIORITY = "showPriorityColumn";
    public static final Boolean PREF_SHOWCOLUMN_PRIORITY_DEFAULT = Boolean.FALSE;

    public static final String PREF_SHOWCOLUMN_RELEVANCE = "showRelevanceColumn";
    public static final Boolean PREF_SHOWCOLUMN_RELEVANCE_DEFAULT = Boolean.FALSE;

    public static final String PREF_SHOWCOLUMN_QUALITY = "showQualityColumn";
    public static final Boolean PREF_SHOWCOLUMN_QUALITY_DEFAULT = Boolean.FALSE;

    public static final String PREF_SHOWCOLUMN_READ = "showReadColumn";
    public static final Boolean PREF_SHOWCOLUMN_READ_DEFAULT = Boolean.FALSE;

    public static final String PREF_SHOWCOLUMN_PRINTED = "showPrintedColumn";
    public static final Boolean PREF_SHOWCOLUMN_PRINTED_DEFAULT = Boolean.FALSE;

    // The choice between PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS and PREF_SERIALIZESPECIALFIELDS is mutually exclusive
    // At least in the settings, not in the implementation. But having both confused the users, therefore, having activated both options at the same time has been disabled
    public static final String PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS = "autoSyncSpecialFieldsToKeywords";
    public static final Boolean PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS_DEFAULT = Boolean.TRUE;

    // The choice between PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS and PREF_SERIALIZESPECIALFIELDS is mutually exclusive
    public static final String PREF_SERIALIZESPECIALFIELDS = "serializeSpecialFields";
    public static final Boolean PREF_SERIALIZESPECIALFIELDS_DEFAULT = Boolean.FALSE;


    /****************************************************/
    /** generic treatment                              **/
    /** no special treatment any more, thanks to enums **/
    /****************************************************/

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
        SpecialFieldsUtils.exportFieldToKeywords(e, be.getField(e.getFieldName()), be, ce);
    }

    private static void exportFieldToKeywords(SpecialField e, BibEntry be, NamedCompound ce) {
        SpecialFieldsUtils.exportFieldToKeywords(e, be.getField(e.getFieldName()), be, ce);
    }

    private static void exportFieldToKeywords(SpecialField e, String newValue, BibEntry be, NamedCompound ce) {
        if (!SpecialFieldsUtils.keywordSyncEnabled()) {
            return;
        }
        List<String> keywordList = be.getSeparatedKeywords();
        List<String> values = e.getKeyWords();

        int foundPos = -1;

        // cleanup keywords
        for (Object value : values) {
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
        String oldValue = be.getField(KEYWORDS_FIELD);
        be.putKeywords(keywordList);
        String updatedValue = be.getField(KEYWORDS_FIELD);
        if (((oldValue == null) || !oldValue.equals(updatedValue)) && (ce != null)) {
            ce.addEdit(new UndoableFieldChange(be, KEYWORDS_FIELD, oldValue, updatedValue));
        }

    }

    /**
     * Update keywords according to values of special fields
     *
     * @param nc indicates the undo named compound. May be null
     */
    public static void syncKeywordsFromSpecialFields(BibEntry be, NamedCompound nc) {
        SpecialFieldsUtils.exportFieldToKeywords(Priority.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Rank.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Relevance.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Quality.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(ReadStatus.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Printed.getInstance(), be, nc);
    }

    private static void importKeywordsForField(List<String> keywordList, SpecialField c, BibEntry be,
            NamedCompound nc) {
        List<String> values = c.getKeyWords();
        String newValue = null;
        for (String val : values) {
            if (keywordList.contains(val)) {
                newValue = val;
                break;
            }
        }
        UpdateField.updateNonDisplayableField(be, c.getFieldName(), newValue)
                .ifPresent(fieldChange -> nc.addEdit(new UndoableFieldChange(fieldChange)));
    }

    /**
     * updates field values according to keywords
     *
     * @param ce indicates the undo named compound. May be null
     */
    public static void syncSpecialFieldsFromKeywords(BibEntry be, NamedCompound ce) {
        if (!be.hasField(KEYWORDS_FIELD)) {
            return;
        }
        List<String> keywordList = net.sf.jabref.model.entry.EntryUtil
                .getSeparatedKeywords(be.getField(KEYWORDS_FIELD));
        SpecialFieldsUtils.importKeywordsForField(keywordList, Priority.getInstance(), be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Rank.getInstance(), be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Quality.getInstance(), be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Relevance.getInstance(), be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, ReadStatus.getInstance(), be, ce);
        SpecialFieldsUtils.importKeywordsForField(keywordList, Printed.getInstance(), be, ce);
    }

    /**
     * @param fieldName the fieldName
     * @return an instance of that field. The returned object is a singleton. null is returned if fieldName does not indicate a special field
     */
    public static SpecialField getSpecialFieldInstanceFromFieldName(String fieldName) {
        if (fieldName.equals(SpecialFieldsUtils.FIELDNAME_PRIORITY)) {
            return Priority.getInstance();
        } else if (fieldName.equals(SpecialFieldsUtils.FIELDNAME_QUALITY)) {
            return Quality.getInstance();
        } else if (fieldName.equals(SpecialFieldsUtils.FIELDNAME_RANKING)) {
            return Rank.getInstance();
        } else if (fieldName.equals(SpecialFieldsUtils.FIELDNAME_RELEVANCE)) {
            return Relevance.getInstance();
        } else if (fieldName.equals(SpecialFieldsUtils.FIELDNAME_READ)) {
            return ReadStatus.getInstance();
        } else if (fieldName.equals(SpecialFieldsUtils.FIELDNAME_PRINTED)) {
            return Printed.getInstance();
        } else {
            return null;
        }
    }

    /**
     * @param fieldName the name of the field to check
     * @return true if given field is a special field, false otherwise
     */
    public static boolean isSpecialField(String fieldName) {
        return SpecialFieldsUtils.getSpecialFieldInstanceFromFieldName(fieldName) != null;
    }

    public static boolean keywordSyncEnabled() {
        return Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED) &&
                Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS);
    }

}
