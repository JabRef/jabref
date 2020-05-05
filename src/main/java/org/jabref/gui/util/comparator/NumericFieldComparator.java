package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;

/**
 * Comparator for numeric cases.
 */
public class NumericFieldComparator implements Comparator<Optional<SpecialFieldValueViewModel>> {

    @Override
    public int compare(Optional<SpecialFieldValueViewModel> val1, Optional<SpecialFieldValueViewModel> val2) {
        if (val1.isPresent()) {
            if (val2.isPresent()) {
                String str1 = String.valueOf(val1);
                String str2 = String.valueOf(val2);
                if (str1.matches("^[0-9]+$") && str2.matches("^[0-9]+$")) {
                    return Integer.parseInt(str1) - Integer.parseInt(str2);
                } else if (!str1.matches("^[0-9]+$") && str2.matches("^[0-9]+$")) {
                    return -1;
                } else if (str1.matches("^[0-9]+$") && !str2.matches("^[0-9]+$")) {
                    return 0;
                } else {
                    return val1.get().getValue().compareTo(val2.get().getValue());
                }
            } else {
                return 1;
            }
        } else {
            if (val2.isPresent()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

}
