/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.bibtex.comparator;

import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.gui.maintable.MainTableFormat;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.entry.BibEntry;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 *
 * A comparator for BibEntry fields
 *
 * Initial Version:
 *
 * @author alver
 * @version Date: Oct 13, 2005 Time: 10:10:04 PM To
 *
 * TODO: Testcases
 *
 */
public class FieldComparator implements Comparator<BibEntry> {

    private static final Collator COLLATOR = getCollator();

    private static Collator getCollator() {
        try {
            return new RuleBasedCollator(
                    ((RuleBasedCollator) Collator.getInstance()).getRules().replace("<'\u005f'", "<' '<'\u005f'"));
        } catch (ParseException e) {
            return Collator.getInstance();
        }
    }

    enum FieldType {
        NAME, TYPE, YEAR, MONTH, OTHER;
    }

    private final String[] field;
    private final String fieldName;
    private final FieldType fieldType;
    private final boolean isNumeric;
    private final int multiplier;

    public FieldComparator(String field) {
        this(field, false);
    }

    public FieldComparator(String field, boolean reversed) {
        this.fieldName = Objects.requireNonNull(field);
        this.field = fieldName.split(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
        fieldType = determineFieldType();
        isNumeric = InternalBibtexFields.isNumeric(this.field[0]);

        if(fieldType == FieldType.MONTH) {
            /*
             * [ 1598777 ] Month sorting
             *
             * http://sourceforge.net/tracker/index.php?func=detail&aid=1598777&group_id=92314&atid=600306
             */
            multiplier = reversed ? 1 : -1;
        } else {
            multiplier = reversed ? -1 : 1;
        }
    }

    private FieldType determineFieldType() {
        if(BibEntry.TYPE_HEADER.equals(this.field[0])) {
            return FieldType.TYPE;
        } else if (InternalBibtexFields.getFieldExtras(this.field[0]).contains(FieldProperties.PERSON_NAMES)) {
            return FieldType.NAME;
        } else if ("year".equals(this.field[0])) {
            return FieldType.YEAR;
        } else if("month".equals(this.field[0])) {
            return FieldType.MONTH;
        } else {
            return FieldType.OTHER;
        }
    }

    public FieldComparator(SaveOrderConfig.SortCriterion sortCriterion) {
        this(sortCriterion.field, sortCriterion.descending);
    }

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        String f1;
        String f2;

        if (fieldType == FieldType.TYPE) {
            // Sort by type.
            f1 = e1.getType();
            f2 = e2.getType();
        } else {
            // If the field is author or editor, we rearrange names so they are
            // sorted according to last name.
            f1 = getField(e1);
            f2 = getField(e2);
        }

        // Catch all cases involving null:
        if ((f1 == null) && (f2 == null)) {
            return 0;
        } else if(f1 == null) {
            return multiplier;
        } else if (f2 == null) {
            return -multiplier;
        }

        // Now we now that both f1 and f2 are != null
        if (fieldType == FieldType.NAME) {
            f1 = AuthorList.fixAuthorForAlphabetization(f1);
            f2 = AuthorList.fixAuthorForAlphabetization(f2);
        } else if (fieldType == FieldType.YEAR) {
            Integer f1year = StringUtil.intValueOfWithNull(f1);
            Integer f2year = StringUtil.intValueOfWithNull(f2);
            int comparisonResult = Integer.compare(f1year == null ? 0 : f1year, f2year == null ? 0 : f2year);
            return comparisonResult * multiplier;
        } else if (fieldType == FieldType.MONTH) {
            return Integer.compare(MonthUtil.getMonth(f1).number, MonthUtil.getMonth(f2).number) * multiplier;
        }

        if (isNumeric) {
            Integer i1 = StringUtil.intValueOfWithNull(f1);
            Integer i2 = StringUtil.intValueOfWithNull(f2);

            if ((i2 != null) && (i1 != null)) {
                // Ok, parsing was successful. Update f1 and f2:
                return i1.compareTo(i2) * multiplier;
            } else if (i1 != null) {
                // The first one was parseable, but not the second one.
                // This means we consider one < two
                return -1 * multiplier;
            } else if (i2 != null) {
                // The second one was parseable, but not the first one.
                // This means we consider one > two
                return 1 * multiplier;
            }
            // Else none of them were parseable, and we can fall back on comparing strings.
        }

        String ours = f1.toLowerCase(Locale.ENGLISH);
        String theirs = f2.toLowerCase(Locale.ENGLISH);
        return COLLATOR.compare(ours, theirs) * multiplier;
    }

    private String getField(BibEntry entry) {
        for (String aField : field) {
            String o = entry.getFieldOrAlias(aField);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    /**
     * Returns the field this Comparator compares by.
     *
     * @return The field name.
     */
    public String getFieldName() {
        return fieldName;
    }
}
