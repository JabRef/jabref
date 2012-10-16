/*  Copyright (C) 2012 JabRef contributors.
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

import net.sf.jabref.Util;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.undo.NamedCompound;

public class SpecialFieldsUtils {
	
	public final static String FIELDNAME_PRIORITY = "priority";
	public final static String FIELDNAME_RANKING = "ranking";
	public final static String FIELDNAME_RELEVANCE = "relevance";
	public final static String FIELDNAME_QUALITY = "qualityassured";

	public final static String  PREF_SPECIALFIELDSENABLED = "specialFieldsEnabled";
	public final static Boolean PREF_SPECIALFIELDSENABLED_DEFAULT = Boolean.FALSE;
	
	public final static String  PREF_SHOWCOLUMN_RANKING = "showRankingColumn";
	public final static Boolean PREF_SHOWCOLUMN_RANKING_DEFAULT = Boolean.TRUE;
	
	public final static String  PREF_RANKING_COMPACT = "compactRankingColumn";
	public final static Boolean PREF_RANKING_COMPACT_DEFAULT = Boolean.TRUE;

	public final static String  PREF_SHOWCOLUMN_PRIORITY = "showPriorityColumn";
	public final static Boolean PREF_SHOWCOLUMN_PRIORITY_DEFAULT = Boolean.FALSE;

	public final static String  PREF_SHOWCOLUMN_RELEVANCE = "showRelevanceColumn";
	public final static Boolean PREF_SHOWCOLUMN_RELEVANCE_DEFAULT = Boolean.FALSE;
	
	public final static String  PREF_SHOWCOLUMN_QUALITY = "showQualityColumn";
	public final static Boolean PREF_SHOWCOLUMN_QUALITY_DEFAULT = Boolean.FALSE;
	
	public final static String  PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS = "autoSyncSpecialFieldsToKeywords";
	public final static Boolean PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS_DEFAULT = Boolean.FALSE;
	
	public final static String  PREF_SERIALIZESPECIALFIELDS = "serializeSpecialFields";
	public final static Boolean PREF_SERIALIZESPECIALFIELDS_DEFAULT = Boolean.TRUE;
	
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
	public static void updateField(SpecialField e, String value, BibtexEntry be, NamedCompound ce, boolean nullFieldIfValueIsTheSame) {
		Util.updateField(be, e.getFieldName(), value, ce, nullFieldIfValueIsTheSame);
		// we cannot use "value" here as updateField has side effects: "nullFieldIfValueIsTheSame" nulls the field if value is the same
		exportFieldToKeywords(e, be.getField(e.getFieldName()), be, ce);
	}
	
	private static void exportFieldToKeywords(SpecialField e, BibtexEntry be, NamedCompound ce) {
		exportFieldToKeywords(e, be.getField(e.getFieldName()), be, ce);
	}
	
	private static void exportFieldToKeywords(SpecialField e, String newValue, BibtexEntry be, NamedCompound ce) {
		if (!SpecialFieldsUtils.keywordSyncEnabled()) return;
		ArrayList<String> keywordList = Util.getSeparatedKeywords(be);
		List<String> values = e.getKeyWords();
		
		int foundPos = -1;
		
		// cleanup keywords
		for (Object value: values) {
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
		Util.putKeywords(be, keywordList, ce);
	}
	
	
	/**
	 * Update keywords according to values of special fields
	 * 
	 * @param nc indicates the undo named compound. May be null
	 */
	public static void syncKeywordsFromSpecialFields(BibtexEntry be, NamedCompound nc) {
		exportFieldToKeywords(Priority.getInstance(), be, nc);
		exportFieldToKeywords(Rank.getInstance(), be, nc);
		exportFieldToKeywords(Relevance.getInstance(), be, nc);
		exportFieldToKeywords(Quality.getInstance(), be, nc);
	}
	
	private static void importKeywordsForField(ArrayList<String> keywordList, SpecialField c, BibtexEntry be, NamedCompound nc) {
		List<String> values = c.getKeyWords();
		String newValue = null;
		for (String val: values) {
			if (keywordList.contains(val)) {
				newValue = val;
				break;
			}
		}
		Util.updateField(be, c.getFieldName(), newValue, nc);
	}

	/**
	 * updates field values according to keywords
	 * 
	 * @param nc indicates the undo named compound. May be null
	 */
	public static void syncSpecialFieldsFromKeywords(BibtexEntry be, NamedCompound ce) {
		if (be.getField("keywords") == null)
			return;
		ArrayList<String> keywordList = Util.getSeparatedKeywords(be.getField("keywords"));
		importKeywordsForField(keywordList, Priority.getInstance(), be, ce);
		importKeywordsForField(keywordList, Rank.getInstance(), be, ce);
		importKeywordsForField(keywordList, Quality.getInstance(), be, ce);
		importKeywordsForField(keywordList, Relevance.getInstance(), be, ce);
	}
	
	/**
	 * @param fieldName the fieldName
	 * @return an instance of that field. The returned object is a singleton. null is returned if fieldName does not indicate a special field
	 */
	public static SpecialField getSpecialFieldInstanceFromFieldName(String fieldName) {
		if (fieldName.equals(FIELDNAME_PRIORITY)) {
			return Priority.getInstance();
		} else if (fieldName.equals(FIELDNAME_QUALITY)) {
			return Quality.getInstance();
		} else if (fieldName.equals(FIELDNAME_RANKING)) {
			return Rank.getInstance();
		} else if (fieldName.equals(FIELDNAME_RELEVANCE)) {
			return Relevance.getInstance();
		} else {
			return null;
		}
	}
	
	/**
	 * @param fieldName the name of the field to check
	 * @return true if given field is a special field, false otherwise
	 */
	public static boolean isSpecialField(String fieldName) {
		return (getSpecialFieldInstanceFromFieldName(fieldName) != null);
	}
	
	public static boolean keywordSyncEnabled() {
		return  Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED) &&
				Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS);
	}

}
