package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Compares Bibtex entries based on their 'crossref' fields. Entries including
 * this field are deemed smaller than entries without this field. This serves
 * the purpose of always placing referenced entries after referring entries in
 * the .bib file. After this criterion comes comparisons of individual fields.
 */
public class CrossRefEntryComparator implements Comparator<BibEntry> {

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        boolean crEntry1 = e1.hasField(StandardField.CROSSREF);
        boolean crEntry2 = e2.hasField(StandardField.CROSSREF);

        if ((crEntry1 && crEntry2) || (!crEntry1 && !crEntry2)) {
            return 0;
        }

        if (!crEntry1) {
            return 1;
        } else {
            return -1;
        }
    }
}
