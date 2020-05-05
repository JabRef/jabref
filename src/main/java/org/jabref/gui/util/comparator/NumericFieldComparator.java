package org.jabref.gui.util.comparator;

import java.util.Comparator;

/**
 * Comparator for numeric cases.
 */
public class NumericFieldComparator implements Comparator<String> {

    @Override
    public int compare(String val1, String val2) {
        /*
         * The purpose of this class is to add the numeric comparison, because values are sorted
         * as if they were strings.
         * */

        // We start by implementing the comparison in the edge cases (if one of the values is null)
        if (val1 == null && val2 == null) return 0;
        if (val1 == null) return -1; // (we assume that "null" is "less than" any other value)
        if (val2 == null) return 1;

        // Now we start the conversion to integers.
        Integer valInt1 = null;
        Integer valInt2 = null;
        try {
            valInt1 = Integer.parseInt(val1.trim()); // In case the user added an unnecessary white space (e.g. 1 1 instead of 11)
        } catch(NumberFormatException ignore) {} // do nothing
        try {
            valInt2 = Integer.parseInt(val2.trim());
        } catch(NumberFormatException ignore) {}
        if (valInt1 == null && valInt2 == null) return val1.compareTo(val2); // None of the values were parsed (i.e both are not numeric)
        // so we will use the normal string comparison.
        if (valInt1 == null) return -1; // We assume that strings "are less" than integers
        if (valInt2 == null) return 1;

        // If we arrive at this stage then both values are actually numeric !
        return valInt1-valInt2;
    }



}
