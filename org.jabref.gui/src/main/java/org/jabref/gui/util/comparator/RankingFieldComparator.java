package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.specialfields.SpecialField;

/**
 * Comparator that handles the ranking icon column
 *
 * Based on IconComparator
 * Only comparing ranking field
 * inverse comparison of ranking as rank5 is higher than rank1
 */
public class RankingFieldComparator implements Comparator<BibEntry> {

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        Optional<String> val1 = e1.getField(SpecialField.RANKING.getFieldName());
        Optional<String> val2 = e2.getField(SpecialField.RANKING.getFieldName());
        if (val1.isPresent()) {
            if (val2.isPresent()) {
                // val1 is not null AND val2 is not null
                int compareToRes = val1.get().compareTo(val2.get());
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
