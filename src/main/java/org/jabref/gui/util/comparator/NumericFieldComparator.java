package org.jabref.gui.util.comparator;

import java.util.Comparator;

/**
 * Comparator for numeric cases.
 */
public class NumericFieldComparator implements Comparator<String> {

    @Override
    public int compare(String val1, String val2) {
        if (!val1.isEmpty()) {
            if (!val2.isEmpty()) {
                if (val1.matches("^[0-9]+$") && val2.matches("^[0-9]+$")) {
                    return Integer.parseInt(val1) - Integer.parseInt(val2);
                } else if (!val1.matches("^[0-9]+$") && val2.matches("^[0-9]+$")) {
                    return -1;
                } else if (val1.matches("^[0-9]+$") && !val2.matches("^[0-9]+$")) {
                    return 0;
                } else {
                    return val1.compareTo(val2);
                }
            } else {
                return 1;
            }
        } else {
            if (!val2.isEmpty()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

}
