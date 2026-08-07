package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;

public class SpecialFieldComparator implements Comparator<Optional<SpecialFieldValueViewModel>> {

    @Override
    public int compare(Optional<SpecialFieldValueViewModel> val1, Optional<SpecialFieldValueViewModel> val2) {
        if (val1.isPresent()) {
            if (val2.isPresent()) {
                return val1.get().getValue().compareTo(val2.get().getValue());
            } else {
                return -1;
            }
        } else {
            if (val2.isPresent()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
