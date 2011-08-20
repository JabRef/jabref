package net.sf.jabref.oo;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.FieldComparator;

import java.util.Comparator;

/**
 * Comparator for sorting bibliography entries.
 *
 * TODO: is it sufficient with a hardcoded sort algorithm for the bibliography?
 */
public class AlphanumericComparator implements Comparator<BibtexEntry> {

    FieldComparator authComp = new FieldComparator("author"),
        editorComp = new FieldComparator("editor"),
        yearComp = new FieldComparator("year");

    public AlphanumericComparator() {

    }

    public int compare(BibtexEntry o1, BibtexEntry o2) {
        // Author as first criterion:
        int comp = authComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // TODO: Is it a good idea to try editor if author fields are equal?
        comp = editorComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Year as next criterion:
        return yearComp.compare(o1, o2);

    }
}
