package net.sf.jabref.oo;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.FieldComparator;

import java.util.Comparator;

/**
 * Comparator for sorting bibliography entries according to publication year. This is used to
 * sort entries in multiple citations where the oldest publication should appear first.
 */
public class YearComparator implements Comparator<BibtexEntry> {

    FieldComparator authComp = new FieldComparator("author"),
        editorComp = new FieldComparator("editor"),
        yearComp = new FieldComparator("year");

    public YearComparator() {

    }

    public int compare(BibtexEntry o1, BibtexEntry o2) {
        // Year as first criterion:
        int comp = yearComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // TODO: Is it a good idea to try editor if author fields are equal?
        // Author as next criterion:
        comp = authComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Editor as next criterion:
        return editorComp.compare(o1, o2);

    }
}
