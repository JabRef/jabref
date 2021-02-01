package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;

/**
 * Comparator for rankings.
 * <p>
 * Inverse comparison of ranking as rank5 is higher than rank1
 */
public class RankingFieldComparator implements Comparator<Optional<SpecialFieldValueViewModel>> {

    @Override
    public int compare(Optional<SpecialFieldValueViewModel> val1, Optional<SpecialFieldValueViewModel> val2) {
        if (val1.isPresent()) {
            if (val2.isPresent()) {
                int compareToRes = val1.get().getValue().compareTo(val2.get().getValue());
                if (compareToRes == 0) {
                    return 0;
                } else {
                    return compareToRes * -1;
                }
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
