package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;
import java.util.Iterator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * Sorts entries by the number of fields and then by the field names.
 */
public class BibEntryByFieldsComparator implements Comparator<BibEntry> {
    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        int sizeComparison = e1.getFields().size() - e2.getFields().size();
        if (sizeComparison != 0) {
            return sizeComparison;
        }
        Iterator<String> it1 = e1.getFields().stream().map(Field::getName).sorted().iterator();
        Iterator<String> it2 = e2.getFields().stream().map(Field::getName).sorted().iterator();
        while (it1.hasNext() && it2.hasNext()) {
            int fieldComparison = it1.next().compareTo(it2.next());
            if (fieldComparison != 0) {
                return fieldComparison;
            }
        }
        assert !it1.hasNext() && !it2.hasNext();
        return 0;
    }
}
