package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Comparator for numeric cases. The purpose of this class is to add the numeric comparison, because values are sorted
 * as if they were strings.
 */
public class NumericFieldComparator implements Comparator<String> {
    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

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

        boolean isVal1Valid = pattern.matcher(val1).matches();
        boolean isVal2Valid = pattern.matcher(val2).matches();
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
}
