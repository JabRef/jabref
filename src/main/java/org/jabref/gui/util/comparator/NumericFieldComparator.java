package org.jabref.gui.util.comparator;

import java.util.Comparator;

/**
 * Comparator for numeric cases. The purpose of this class is to add the numeric comparison, because values are sorted
 * as if they were strings.
 */
public class NumericFieldComparator implements Comparator<String> {

    @Override
    public int compare(String val1, String val2) {
        // We start by implementing the comparison in the edge cases (if one of the values is null).
        if (val1 == null && val2 == null) {
            return 0;
        }

        if (val1 == null) {
            // We assume that "null" is "less than" any other value.
            return -1;
        }

        if (val2 == null) {
            return 1;
        }

        // Now we start the conversion to integers.
        Integer valInt1 = null;
        Integer valInt2 = null;
        try {
            // Trim in case the user added an unnecessary white space (e.g. 1 1 instead of 11).
            valInt1 = Integer.parseInt(val1.trim());
        } catch (NumberFormatException ignore) {
            // do nothing
        }
        try {
            valInt2 = Integer.parseInt(val2.trim());
        } catch (NumberFormatException ignore) {
            // do nothing
        }

        if (valInt1 == null && valInt2 == null) {
            // None of the values were parsed (i.e both are not numeric)
            // so we will use the normal string comparison.
            return val1.compareTo(val2);
        }

        if (valInt1 == null) {
            // We assume that strings "are less" than integers
            return -1;
        }

        if (valInt2 == null) {
            return 1;
        }

        // If we arrive at this stage then both values are actually numeric !
        return valInt1 - valInt2;
    }
}
