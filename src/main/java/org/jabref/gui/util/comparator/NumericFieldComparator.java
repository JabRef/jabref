package org.jabref.gui.util.comparator;

import java.util.Comparator;

import org.jabref.model.strings.StringUtil;

/**
 * Comparator for numeric cases. The purpose of this class is to add the numeric comparison, because values are sorted
 * as if they were strings.
 */
public class NumericFieldComparator implements Comparator<String> {

    @Override
    public int compare(String val1, String val2) {
        if ((val1 == null) && (val2 == null)) {
            return 0;
        }

        // Similar implementation as in {@link org.jabref.logic.bibtex.comparator.FieldComparator.compare}.
        int i1;
        boolean i1present;
        try {
            i1 = StringUtil.intValueOf(val1);
            i1present = true;
        } catch (NumberFormatException ex) {
            i1 = 0;
            i1present = false;
        }
        int i2;
        boolean i2present;
        try {
            i2 = StringUtil.intValueOf(val2);
            i2present = true;
        } catch (NumberFormatException ex) {
            i2 = 0;
            i2present = false;
        }

        if (i1present && i2present) {
            return i1 - i2;
        } else if (i1present) {
            // The first one was parsable, but not the second one.
            // This means we consider one < two
            // We assume that "null" is "less than" any other value.
            return 1;
        } else if (i2present) {
            // The second one was parsable, but not the first one.
            // This means we consider one > two
            // We assume that "null" is "less than" any other value.
            return -1;
        } else {
            if (val1 != null && val2 != null) {
                return val1.compareTo(val2);
            } else if (val1 == null) {
                return -1;
            } else if (val2 == null) {
                return 1;
            } else {
                return CharSequence.compare(val1, val2);
            }
        }
    }
}
