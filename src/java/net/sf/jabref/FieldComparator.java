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
package net.sf.jabref;

import net.sf.jabref.gui.MainTableFormat;

import java.text.Collator;
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
 * Current Version:
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 * TODO: Testcases
 * 
 */
public class FieldComparator implements Comparator<BibtexEntry> {

    private static Collator collator;

    static {
        collator = Collator.getInstance();
    }

	private String[] field;
    private String fieldName;

    boolean isNameField, isTypeHeader, isYearField, isMonthField, isNumeric;

	int multiplier;

	public FieldComparator(String field) {
		this(field, false);
	}

	public FieldComparator(String field, boolean reversed) {
        this.fieldName = field;
        this.field = field.split(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
		multiplier = reversed ? -1 : 1;
		isTypeHeader = this.field[0].equals(GUIGlobals.TYPE_HEADER);
        isNameField = (this.field[0].equals("author")
                || this.field[0].equals("editor"));
		isYearField = this.field[0].equals("year");
		isMonthField = this.field[0].equals("month");
        isNumeric = BibtexFields.isNumeric(this.field[0]);
    }

	public int compare(BibtexEntry e1, BibtexEntry e2) {
		Object f1, f2;

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
		if (isMonthField)
			localMultiplier = -localMultiplier;
		
		// Catch all cases involving null:
		if (f1 == null)
			return f2 == null ? 0 : localMultiplier;

		if (f2 == null)
			return -localMultiplier;

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
			f1 = Util.toFourDigitYear((String) f1);
			f2 = Util.toFourDigitYear((String) f2);
		} else if (isMonthField) {
			/*
			 * [ 1535044 ] Month sorting
			 * 
			 * http://sourceforge.net/tracker/index.php?func=detail&aid=1535044&group_id=92314&atid=600306
			 */
			f1 = new Integer(Util.getMonthNumber((String)f1));			
			f2 = new Integer(Util.getMonthNumber((String)f2));
		}

        if (isNumeric) {
            Integer i1 = null, i2 = null;
            try {
                i1 = Integer.parseInt((String)f1);
            } catch (NumberFormatException ex) {
                // Parsing failed.
            }

            try {
                i2 = Integer.parseInt((String)f2);
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
                f2 = new Integer(i1.intValue()+1);
            } else if (i2 != null) {
                // The second one was parseable, but not the first one.
                // This means we consider one > two
                f2 = i2;
                f1 = new Integer(i2.intValue()+1);
            }
            // Else none of them were parseable, and we can fall back on comparing strings.    
        }

        int result = 0;
		if ((f1 instanceof Integer) && (f2 instanceof Integer)) {
			result = (((Integer) f1).compareTo((Integer) f2));
		} else if (f2 instanceof Integer) {
			Integer f1AsInteger = new Integer(f1.toString());
			result = -((f1AsInteger).compareTo((Integer) f2));
		} else if (f1 instanceof Integer) {
			Integer f2AsInteger = new Integer(f2.toString());
			result = -(((Integer) f1).compareTo(f2AsInteger));
		} else {
			String ours = ((String) f1).toLowerCase(), theirs = ((String) f2).toLowerCase();
            result = collator.compare(ours, theirs);//ours.compareTo(theirs);
		}

		return result * localMultiplier;
	}

    private Object getField(BibtexEntry entry) {
        for (int i = 0; i < field.length; i++) {
            Object o = entry.getField(field[i]);
            if (o != null)
                return o;
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
