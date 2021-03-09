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
        Integer valInt1 = parseInt(val1);
        Integer valInt2 = parseInt(val2);

        if (valInt1 == null && valInt2 == null) {
            if (val1 != null && val2 != null) {
                return val1.compareTo(val2);
            } else {
                return 0;
            }
        } else if (valInt1 == null) {
            // We assume that "null" is "less than" any other value.
            return -1;
        } else if (valInt2 == null) {
            return 1;
        }

        // If we arrive at this stage then both values are actually numeric !
        return valInt1 - valInt2;
    }

    private static Integer parseInt(String number) {
        if (!isNumber(number)) {
            return null;
        }

        try {
            return Integer.valueOf(number.trim());
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private static boolean isNumber(String number) {
        if (StringUtil.isNullOrEmpty(number)) {
            return false;
        }
        if (number.length() == 1 && (number.charAt(0) == '-' || number.charAt(0) == '+')) {
            return false;
        }
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (i == 0 && (c == '-' || c == '+')) {
                continue;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }
}
