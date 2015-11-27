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

import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.gui.MainTableFormat;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.entry.YearUtil;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.util.Util;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;

/**
 * 
 * A comparator for BibtexEntry fields
 * 
 * Initial Version:
 * 
 * @author alver
 * @version Date: Oct 13, 2005 Time: 10:10:04 PM To
 * 
 * TODO: Testcases
 * 
 */
public class FieldComparator implements Comparator<BibtexEntry> {

    private static Collator collator;

    static {
        try {
            FieldComparator.collator = new RuleBasedCollator(
                    ((RuleBasedCollator) Collator.getInstance()).getRules()
                            .replaceAll("<'\u005f'", "<' '<'\u005f'"));
        } catch (ParseException e) {
            FieldComparator.collator = Collator.getInstance();
        }
    }

    private final String[] field;
    private final String fieldName;

    private final boolean isNameField;
    private final boolean isTypeHeader;
    private final boolean isYearField;
    private final boolean isMonthField;
    private final boolean isNumeric;

    private final int multiplier;


    public FieldComparator(String field) {
        this(field, false);
    }

    public FieldComparator(String field, boolean reversed) {
        this.fieldName = field;
        this.field = field.split(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
        multiplier = reversed ? -1 : 1;
        isTypeHeader = this.field[0].equals(BibtexEntry.TYPE_HEADER);
        isNameField = "author".equals(this.field[0])
                || "editor".equals(this.field[0]);
        isYearField = "year".equals(this.field[0]);
        isMonthField = "month".equals(this.field[0]);
        isNumeric = BibtexFields.isNumeric(this.field[0]);
    }

    @Override
    public int compare(BibtexEntry e1, BibtexEntry e2) {
        Object f1;
        Object f2;

        if (isTypeHeader) {
            // Sort by type.
            f1 = e1.getType().getName();
            f2 = e2.getType().getName();
        } else {

            // If the field is author or editor, we rearrange names so they are
            // sorted according to last name.
            f1 = getField(e1);
            f2 = getField(e2);
        }

        /*
         * [ 1598777 ] Month sorting
         * 
         * http://sourceforge.net/tracker/index.php?func=detail&aid=1598777&group_id=92314&atid=600306
         */
        int localMultiplier = multiplier;
        if (isMonthField) {
            localMultiplier = -localMultiplier;
        }

        // Catch all cases involving null:
        if (f1 == null) {
            return f2 == null ? 0 : localMultiplier;
        }

        if (f2 == null) {
            return -localMultiplier;
        }

        // Now we now that both f1 and f2 are != null
        if (isNameField) {
            f1 = AuthorList.fixAuthorForAlphabetization((String) f1);
            f2 = AuthorList.fixAuthorForAlphabetization((String) f2);
        } else if (isYearField) {
            /*
             * [ 1285977 ] Impossible to properly sort a numeric field
             * 
             * http://sourceforge.net/tracker/index.php?func=detail&aid=1285977&group_id=92314&atid=600307
             */
            f1 = YearUtil.toFourDigitYear((String) f1);
            f2 = YearUtil.toFourDigitYear((String) f2);
        } else if (isMonthField) {
            /*
             * [ 1535044 ] Month sorting
             * 
             * http://sourceforge.net/tracker/index.php?func=detail&aid=1535044&group_id=92314&atid=600306
             */
            f1 = MonthUtil.getMonth((String) f1).number;
            f2 = MonthUtil.getMonth((String) f2).number;
        }

        if (isNumeric) {
            Integer i1 = null;
            Integer i2 = null;
            try {
                i1 = Util.intValueOf((String) f1);
            } catch (NumberFormatException ex) {
                // Parsing failed.
            }

            try {
                i2 = Util.intValueOf((String) f2);
            } catch (NumberFormatException ex) {
                // Parsing failed.
            }

            if (i2 != null && i1 != null) {
                // Ok, parsing was successful. Update f1 and f2:
                f1 = i1;
                f2 = i2;
            } else if (i1 != null) {
                // The first one was parseable, but not the second one.
                // This means we consider one < two
                f1 = i1;
                f2 = i1 + 1;
            } else if (i2 != null) {
                // The second one was parseable, but not the first one.
                // This means we consider one > two
                f2 = i2;
                f1 = i2 + 1;
            }
            // Else none of them were parseable, and we can fall back on comparing strings.    
        }

        int result;
        if (f1 instanceof Integer && f2 instanceof Integer) {
            result = ((Integer) f1).compareTo((Integer) f2);
        } else if (f2 instanceof Integer) {
            Integer f1AsInteger = Integer.valueOf(f1.toString());
            result = -f1AsInteger.compareTo((Integer) f2);
        } else if (f1 instanceof Integer) {
            Integer f2AsInteger = Integer.valueOf(f2.toString());
            result = -((Integer) f1).compareTo(f2AsInteger);
        } else {
            String ours = ((String) f1).toLowerCase();
            String theirs = ((String) f2).toLowerCase();
            result = FieldComparator.collator.compare(ours, theirs);//ours.compareTo(theirs);
        }

        return result * localMultiplier;
    }

    private Object getField(BibtexEntry entry) {
        for (String aField : field) {
            Object o = entry.getFieldOrAlias(aField);
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
