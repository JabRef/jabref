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
        } else if (val1 == null) {
            // We assume that "null" is "less than" any other value.
            return -1;
        } else if (val2 == null) {
            return 1;
        }

        boolean isVal1Valid = isNumber(val1);
        boolean isVal2Valid = isNumber(val2);
        if (!isVal1Valid && !isVal2Valid) {
            return val1.compareTo(val2);
        } else if (!isVal1Valid) {
            return -1;
        } else if (!isVal2Valid) {
            return 1;
        }

        // Now we start the conversion to integers.
        Integer valInt1 = Integer.parseInt(val1.trim());
        Integer valInt2 = Integer.parseInt(val2.trim());

        // If we arrive at this stage then both values are actually numeric !
        return valInt1 - valInt2;
    }

    private static boolean isNumber(String number) {
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if ((i == 0 && c != '-') && (c < '0' || c > '9')) {
                return false;
            }
        }

        return true;
    }
}
