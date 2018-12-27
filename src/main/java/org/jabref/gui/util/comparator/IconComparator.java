package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Comparator that handles icon columns.
 */
public class IconComparator implements Comparator<BibEntry> {

    private final List<String> fields;


    public IconComparator(List<String> fields) {
        this.fields = fields;
    }

    @Override
    public int compare(BibEntry e1, BibEntry e2) {

        for (String field : fields) {
            Optional<String> val1 = e1.getField(field);
            Optional<String> val2 = e2.getField(field);
            if (val1.isPresent()) {
                if (val2.isPresent()) {
                    // val1 is not null AND val2 is not null
                    int compareToRes = val1.get().compareTo(val2.get());
                    if (compareToRes == 0) {
                        // continue loop as current two values are equal
                    } else {
                        return compareToRes;
                    }
                } else {
                    return -1;
                }
            } else {
                if (val2.isPresent()) {
                    return 1;
                } else {
                    // continue loop and check for next field
                }
            }
        }
        return 0;
    }

}
