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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.SpecialFields;
import net.sf.jabref.preferences.JabRefPreferences;


public class SpecialFieldsUtils {


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
        SpecialFieldsUtils.exportFieldToKeywords(e, be.getFieldOptional(e.getFieldName()).orElse(null), be, ce);
    }

    private static void exportFieldToKeywords(SpecialField e, BibEntry be, NamedCompound ce) {
        SpecialFieldsUtils.exportFieldToKeywords(e, be.getFieldOptional(e.getFieldName()).orElse(null), be, ce);
    }

    private static void exportFieldToKeywords(SpecialField e, String newValue, BibEntry entry,
            NamedCompound ce) {
        if (!SpecialFieldsUtils.keywordSyncEnabled()) {
            return;
        }
        List<String> keywordList = new ArrayList<>(entry.getKeywords());
        List<String> specialFieldsKeywords = e.getKeyWords();

        int foundPos = -1;

        // cleanup keywords
        for (Object value : specialFieldsKeywords) {
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


        Optional<FieldChange> change = entry.putKeywords(keywordList,
                Globals.prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
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
        SpecialFieldsUtils.exportFieldToKeywords(Priority.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Rank.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Relevance.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Quality.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(ReadStatus.getInstance(), be, nc);
        SpecialFieldsUtils.exportFieldToKeywords(Printed.getInstance(), be, nc);
    }

    private static void importKeywordsForField(Set<String> keywordList, SpecialField c, BibEntry be,
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
        Set<String> keywordList = EntryUtil.getSeparatedKeywords(be);
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

    public static boolean keywordSyncEnabled() {
        return Globals.prefs.getBoolean(JabRefPreferences.PREF_SPECIALFIELDSENABLED) &&
                Globals.prefs.getBoolean(JabRefPreferences.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS);
    }

}
